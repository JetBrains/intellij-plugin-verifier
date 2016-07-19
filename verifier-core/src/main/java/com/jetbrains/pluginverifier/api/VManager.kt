package com.jetbrains.pluginverifier.api

import com.google.common.base.Joiner
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.Plugin
import com.intellij.structure.domain.PluginDependency
import com.intellij.structure.errors.IncorrectPluginException
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.CyclicDependenciesProblem
import com.jetbrains.pluginverifier.problems.MissingDependencyProblem
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.utils.dependencies.Dependencies
import com.jetbrains.pluginverifier.utils.dependencies.MissingReason
import com.jetbrains.pluginverifier.utils.dependencies.PluginDependenciesNode
import com.jetbrains.pluginverifier.verifiers.ReferencesVerifier
import org.slf4j.LoggerFactory
import java.io.IOException

interface VProgress {
  fun getProgress(): Double

  fun setProgress(value: Double)

  fun getText(): String

  fun setText(text: String)
}

private val LOG = LoggerFactory.getLogger(VManager::class.java)


/**
 * @author Sergey Patrikeev
 */
object VManager {


  /**
   * Perform the verification according to passed parameters.
   *
   * The parameters consist of the _(ide, plugin)_ pairs.
   * Every plugin is checked with the corresponding IDE.
   * For every such pair the method returns a result of the verification:
   * normally every result consists of the found binary problems (the main task of the Verifier), if they exist.
   * But there could be the verification errors (the most typical are due to missing plugin mandatory dependencies,
   * invalid plugin class-files or whatever other reasons).
   * Thus you should check the type of the _(ide, plugin)_ pairs results (see [VResult]) .
   *
   * @return the verification results
   * @throws InterruptedException if the verification was cancelled
   * @throws RuntimeException if unexpected errors occur: e.g. the IDE is broken, or the Repository doesn't respond.
   */
  @Throws(InterruptedException::class)
  @JvmOverloads
  fun verify(params: VParams, progress: VProgress = DefaultVProgress()): VResults {

    LOG.info("Verifying the plugins according to $params")
    val pluginsNumber = params.pluginsToCheck.size
    progress.setText("Verifying $pluginsNumber plugins")
    progress.setProgress(0.0)
    var verified = 0

    val time = System.currentTimeMillis()

    val results = arrayListOf<VResult>()

    //IOException is propagated. Auto-close the JDK resolver (we have just created it)
    VParamsCreator.createJdkResolver(params.jdkDescriptor).use { runtimeResolver ->

      //Group by IDE to reduce the Ide-Resolver creations number.
      params.pluginsToCheck.groupBy { it.second }.entries.forEach { ideToPlugins ->
        val ideDescriptor = ideToPlugins.key

        checkCancelled()

        LOG.debug("Creating Resolver for $ideDescriptor")

        val ide: Ide
        try {
          ide = VParamsCreator.getIde(ideDescriptor)
        } catch(ie: InterruptedException) {
          throw ie
        } catch(e: Exception) {
          //IDE errors are propagated. We assume the IDE-s are correct while the plugins may not be so.
          throw RuntimeException("Failed to create IDE instance for $ideDescriptor")
        }

        //we must not close the IDE Resolver coming from the caller
        val closeIdeResolver: Boolean
        val ideResolver: Resolver
        try {
          ideResolver = when (ideDescriptor) {
            is IdeDescriptor.ByFile -> {
              closeIdeResolver = true; Resolver.createIdeResolver(ide)
            }
            is IdeDescriptor.ByVersion -> {
              closeIdeResolver = true; Resolver.createIdeResolver(ide)
            }
            is IdeDescriptor.ByInstance -> {
              closeIdeResolver = (ideDescriptor.ideResolver == null)
              if (closeIdeResolver) {
                Resolver.createIdeResolver(ide)
              } else {
                ideDescriptor.ideResolver!!
              }
            }
            IdeDescriptor.AnyIde -> throw IllegalArgumentException()
          }
        } catch(ie: InterruptedException) {
          throw ie
        } catch(e: Exception) {
          //IDE errors are propagated.
          throw RuntimeException("Failed to read IDE classes for $ideDescriptor")
        }

        try {
          ideToPlugins.value.forEach poi@ { pluginOnIde ->
            checkCancelled()

            val text = "Verifying ${pluginOnIde.first.presentableName()} with ${pluginOnIde.second.presentableName()}"
            LOG.info(text)
            progress.setText(text)

            var pluginResult: VResult? = null
            try {
              val plugin: Plugin
              try {
                plugin = VParamsCreator.getPlugin(pluginOnIde.first, ide.version)
              } catch(ie: InterruptedException) {
                throw ie
              } catch(e: IncorrectPluginException) {
                //the plugin has incorrect structure.
                val reason = e.message ?: "The plugin ${pluginOnIde.first} has incorrect structure"
                LOG.error(reason, e)
                pluginResult = VResult.BadPlugin(pluginOnIde.first, reason)
                return@poi
              } catch(e: UpdateNotFoundException) {
                //the caller has specified a missing plugin
                LOG.error(e.message ?: "The plugin ${pluginOnIde.first} is not found in the Repository", e)
                throw e
              } catch(e: IOException) {
                //the plugin has an invalid file
                val reason = e.message ?: e.javaClass.name
                LOG.error(reason, e)
                pluginResult = VResult.BadPlugin(pluginOnIde.first, reason)
                return@poi
              }

              val pluginResolver: Resolver
              try {
                pluginResolver = Resolver.createPluginResolver(plugin)
              } catch(e: Exception) {
                val reason = e.message ?: "Failed to read the class-files of the plugin $plugin"
                LOG.error(reason, e)
                pluginResult = VResult.BadPlugin(pluginOnIde.first, reason)
                return@poi
              }

              //auto-close
              pluginResolver.use puse@ {
                val ctx = VContext(plugin, pluginOnIde.first, ide, pluginOnIde.second, params.options)
                val pair = getDependenciesResolver(ctx)
                if (pair.second != null) {
                  pluginResult = pair.second
                  return@puse
                }

                pair.first!!.use { dependenciesResolver ->
                  val checkClasses = getClassesForCheck(plugin, pluginResolver)
                  val classLoader = createClassLoader(dependenciesResolver, ctx, ideResolver, pluginResolver, runtimeResolver, params.externalClassPath)

                  try {
                    ReferencesVerifier.verify(ctx, checkClasses, classLoader)

                    if (ctx.problems.isEmpty) {
                      pluginResult = VResult.Nice(ctx.pluginDescriptor, ctx.ideDescriptor, ctx.overview)
                    } else {
                      pluginResult = VResult.Problems(ctx.pluginDescriptor, ctx.ideDescriptor, ctx.overview, ctx.problems)
                    }

                    LOG.info("Successfully verified $plugin with ${pluginOnIde.second.presentableName()}")
                  } catch (ie: InterruptedException) {
                    throw ie
                  } catch (e: Exception) {
                    val message = "Failed to verify ${pluginOnIde.first} with ${pluginOnIde.second}"
                    LOG.error(message, e)
                    throw RuntimeException(message, e)
                  }
                }

              }
            } finally {
              val result = pluginResult!!
              results.add(result)
              progress.setText("${pluginOnIde.first.presentableName()} has been verified with ${pluginOnIde.second.presentableName()}. Result: ${
              when (result) {
                is VResult.Nice -> "It is OK."
                is VResult.Problems -> "It has ${result.problems.keySet().size} problems"
                is VResult.BadPlugin -> "It is invalid: ${result.overview}"
              }
              }")
              progress.setProgress(((++verified).toDouble()) / pluginsNumber)
            }

          }
        } finally {
          if (closeIdeResolver) {
            ideResolver.close()
          }
        }
      }
    }

    val elapsed = "${(System.currentTimeMillis() - time) / 1000} seconds"
    LOG.info("The verification has been successfully completed in $elapsed")
    progress.setText("Finished in $elapsed")
    progress.setProgress(1.0)

    return VResults(results)
  }

  private fun checkCancelled() {
    if (Thread.currentThread().isInterrupted) {
      throw InterruptedException("The verification was cancelled")
    }
  }

  private fun getDependenciesResolver(ctx: VContext): Pair<Resolver?, VResult?> {

    val plugin = ctx.plugin
    val dependencies = Dependencies.calcDependencies(plugin, ctx.ide)
    if (dependencies.cycle != null && ctx.verifierOptions.failOnCyclicDependencies) {
      val cycle = Joiner.on(" -> ").join(dependencies.cycle)
      LOG.error("The plugin verifier will not verify a plugin $ctx.plugin because its dependencies tree has the following cycle: $cycle")
      ctx.registerProblem(CyclicDependenciesProblem(cycle), ProblemLocation.fromPlugin(plugin.pluginId))
      return null to VResult.Problems(ctx.pluginDescriptor, ctx.ideDescriptor, "Cyclic dependencies: $cycle", ctx.problems)
    }
    val depNode = dependencies.descriptor

    for ((key, value) in depNode.missingDependencies) {
      val missingId = key.id
      if (!ctx.verifierOptions.isIgnoreDependency(missingId)) {
        ctx.registerProblem(MissingDependencyProblem(plugin.pluginId, missingId, value.reason), ProblemLocation.fromPlugin(plugin.pluginId))
      }
    }

    val missingMandatoryDeps: Map<PluginDependency, MissingReason> = depNode.missingDependencies.filterNot({ it.key.isOptional })
    if (!missingMandatoryDeps.isEmpty()) {
      LOG.error("The plugin verifier will not verify a plugin $plugin because it has missing mandatory dependencies: ${depNode.missingDependencies.entries.joinToString { it.key.toString() }}")
      return null to VResult.Problems(ctx.pluginDescriptor, ctx.ideDescriptor, "Missing dependencies: ${missingMandatoryDeps.map { it.key.id }}", ctx.problems)
    }

    val mandatoryDeps: MutableSet<PluginDependenciesNode> = hashSetOf()
    dfsMandatoryDependencies(depNode, mandatoryDeps)


    if (depNode.transitiveDependencies.isEmpty()) {
      return Resolver.getEmptyResolver() to null
    } else {
      val depResolvers: MutableList<Resolver> = arrayListOf()
      for (dep in depNode.transitiveDependencies) {
        try {
          depResolvers.add(Resolver.createPluginResolver(dep))
        } catch (ie: InterruptedException) {
          depResolvers.forEach { it.close() }
          throw ie
        } catch (e: Exception) {
          val isMandatory = mandatoryDeps.find { it.plugin.equals(dep) } != null
          val message = "Unable to read class-files of the ${if (isMandatory) "mandatory" else "optional"} plugin ${dep.pluginId}"

          ctx.registerProblem(MissingDependencyProblem(plugin.pluginId, dep.pluginId, message), ProblemLocation.fromPlugin(plugin.pluginId))

          if (isMandatory) {
            LOG.error("The plugin verifier will not verify a plugin because its dependent plugin $dep has broken class-files", e)
            depResolvers.forEach { it.close() }
            return null to VResult.Problems(ctx.pluginDescriptor, ctx.ideDescriptor, "Transitive mandatory dependency $dep is unavailable", ctx.problems)
          } else {
            LOG.error(message, e)
          }
        }
      }
      return Resolver.createUnionResolver("Plugin ${depNode.plugin} transitive dependencies", depResolvers) to null
    }

  }

  /**
   * Traverse the dependencies starting from [node]. Only the mandatory (non-optional) dependencies edges are considered.
   */
  private fun dfsMandatoryDependencies(node: PluginDependenciesNode, accumulator: MutableSet<PluginDependenciesNode>) {
    accumulator.add(node)

    val directMandatoryDeps = listOf(*node.plugin.dependencies.toTypedArray(), *node.plugin.moduleDependencies.toTypedArray()).filter { !it.isOptional }

    for (to in node.edges) {
      if (directMandatoryDeps.find { it.id.equals(to.plugin.pluginId) } != null) {
        if (!accumulator.contains(to)) {
          dfsMandatoryDependencies(to, accumulator)
        }
      }
    }

  }


  private fun createClassLoader(dependenciesResolver: Resolver,
                                context: VContext,
                                ideResolver: Resolver,
                                pluginResolver: Resolver,
                                runtimeResolver: Resolver,
                                externalClassPath: Resolver): Resolver =
      Resolver.createCacheResolver(
          Resolver.createUnionResolver(
              "Common resolver for plugin " + context.plugin.pluginId + " with its transitive dependencies; ide " + context.ide.version + "; jdk " + runtimeResolver,
              listOf(pluginResolver, runtimeResolver, ideResolver, dependenciesResolver, externalClassPath)
          )
      )

  private fun getClassesForCheck(plugin: Plugin, pluginResolver: Resolver): Resolver {
    val resolver = Resolver.createUnionResolver("Plugin classes for check",
        (plugin.allClassesReferencedFromXml + plugin.optionalDescriptors.flatMap { it.value.allClassesReferencedFromXml })
            .map { pluginResolver.getClassLocation(it) }
            .filterNotNull()
            .distinct())
    if (resolver.isEmpty) {
      return pluginResolver
    }
    return resolver
  }

}

internal class VContext(
    val plugin: Plugin,
    val pluginDescriptor: PluginDescriptor,
    val ide: Ide,
    val ideDescriptor: IdeDescriptor,
    val verifierOptions: VOptions
) {
  val problems: Multimap<Problem, ProblemLocation> = HashMultimap.create()
  val overview: String = ""

  fun registerProblem(problem: Problem, location: ProblemLocation) {
    if (!verifierOptions.isIgnoredProblem(plugin, problem)) {
      problems.put(problem, location)
    }
  }

}

class DefaultVProgress() : VProgress {
  @Volatile private var progress: Double = 0.0

  @Volatile private var text: String = ""

  override fun getProgress(): Double = progress

  override fun setProgress(value: Double) {
    progress = value
  }

  override fun getText(): String = text

  override fun setText(text: String) {
    this.text = text
  }

}
