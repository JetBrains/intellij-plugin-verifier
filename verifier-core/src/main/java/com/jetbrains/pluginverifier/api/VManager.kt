package com.jetbrains.pluginverifier.api

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
import java.io.Closeable
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

    LOG.debug("Verifying the plugins according to $params")
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

        LOG.trace("Creating Resolver for $ideDescriptor")

        var ide: Ide
        try {
          ide = VParamsCreator.getIde(ideDescriptor)
        } catch(ie: InterruptedException) {
          throw ie
        } catch(e: Exception) {
          //IDE errors are propagated. We assume the IDE-s are correct while the plugins may not be so.
          throw RuntimeException("Failed to create IDE instance for $ideDescriptor")
        }

        val (closeIdeResolver: Boolean, ideResolver: Resolver) = ideResolverPair(ide, ideDescriptor)

        try {
          ideToPlugins.value.forEach poi@ { pluginOnIde ->
            val pluginDescriptor = pluginOnIde.first

            checkCancelled()

            val text = "Verifying ${pluginDescriptor.presentableName()} with ${ideDescriptor.presentableName()}"
            LOG.trace(text)
            progress.setText(text)

            var pluginResult: VResult? = null
            try {
              val plugin: Plugin
              try {
                plugin = VParamsCreator.getPlugin(pluginDescriptor, ide.version)
              } catch(ie: InterruptedException) {
                throw ie
              } catch(e: IncorrectPluginException) {
                //the plugin has incorrect structure.
                val reason = e.message ?: "The plugin ${pluginDescriptor.presentableName()} has incorrect structure"
                LOG.debug(reason, e) //this is a problem of the plugin, but not of the Verifier.
                pluginResult = VResult.BadPlugin(pluginDescriptor, reason)
                return@poi
              } catch(e: UpdateNotFoundException) {
                //the caller has specified a missing plugin
                val reason = e.message ?: "The plugin ${pluginDescriptor.presentableName()} is not found in the Repository"
                LOG.debug(reason, e)
                pluginResult = VResult.NotFound(pluginDescriptor, ideDescriptor, reason)
                return@poi
              } catch(e: IOException) {
                //the plugin has an invalid file
                val reason = e.message ?: e.javaClass.name
                LOG.debug(reason, e)
                pluginResult = VResult.BadPlugin(pluginDescriptor, reason)
                return@poi
              } catch (e: RuntimeException) {
                throw e
              } catch (e: Exception) {
                throw RuntimeException(e)
              }

              assert(pluginResult == null)

              val pluginResolver: Resolver
              try {
                pluginResolver = Resolver.createPluginResolver(plugin)
              } catch (ie: InterruptedException) {
                throw ie
              } catch(e: Exception) {
                val reason = e.message ?: "Failed to read the class-files of the plugin $plugin"
                LOG.debug(reason, e)
                pluginResult = VResult.BadPlugin(pluginDescriptor, reason)
                return@poi
              }

              //auto-close
              pluginResolver.use puse@ {
                val ctx = VContext(plugin, pluginDescriptor, ide, ideDescriptor, params.options)
                val (dependenciesResolver: Resolver?, vResult: VResult?) = getDependenciesResolver(ctx)
                if (vResult != null) {
                  pluginResult = vResult
                  return@puse
                }

                dependenciesResolver!!.use {
                  try {
                    val checkClasses = getClassesForCheck(plugin, pluginResolver)
                    val classLoader = createClassLoader(dependenciesResolver, ctx, ideResolver, pluginResolver, runtimeResolver, params.externalClassPath)

                    ReferencesVerifier.verify(ctx, checkClasses, classLoader)

                    if (ctx.problems.isEmpty) {
                      pluginResult = VResult.Nice(ctx.pluginDescriptor, ctx.ideDescriptor, ctx.overview)
                    } else {
                      pluginResult = VResult.Problems(ctx.pluginDescriptor, ctx.ideDescriptor, ctx.overview, ctx.problems)
                    }

                    if (params.resolveDependenciesWithin) {
                      ide = ide.getExpandedIde(plugin)
                    }

                  } catch (ie: InterruptedException) {
                    throw ie
                  } catch (e: Exception) {
                    val message = "Failed to verify $pluginDescriptor with $ideDescriptor"
                    LOG.error(message, e)
                    throw RuntimeException(message, e)
                  }
                }

              }
            } finally {
              if (pluginResult != null) {
                val result = pluginResult!!
                results.add(result)
                val resType: String = when (result) {
                  is VResult.Nice -> "It is OK."
                  is VResult.Problems -> "It has ${result.problems.keySet().size} problems"
                  is VResult.BadPlugin -> "It is invalid: ${result.overview}"
                  is VResult.NotFound -> "It is not found: ${result.overview}"
                }
                val statusString = "${pluginDescriptor.presentableName()} has been verified with ${ideDescriptor.presentableName()}. Result: $resType"
                progress.setText(statusString)
                progress.setProgress(((++verified).toDouble()) / pluginsNumber)
                LOG.trace("$statusString; progress = $verified out of $pluginsNumber")
              }
            }

          }
        } finally {
          if (closeIdeResolver) {
            ideResolver.closeLogged()
          }
        }
      }
    }

    val elapsed = "${(System.currentTimeMillis() - time) / 1000} seconds"
    LOG.debug("The verification has been successfully completed in $elapsed")
    progress.setText("Finished in $elapsed")
    progress.setProgress(1.0)

    return VResults(results)
  }

  private fun ideResolverPair(ide: Ide, ideDescriptor: IdeDescriptor): Pair<Boolean, Resolver> {
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
    return Pair(closeIdeResolver, ideResolver)
  }

  private fun checkCancelled() {
    if (Thread.currentThread().isInterrupted) {
      throw InterruptedException("The verification was cancelled")
    }
  }

  private fun getDependenciesResolver(ctx: VContext): Pair<Resolver?, VResult?> {

    val plugin = ctx.plugin
    val dependencies: Dependencies.DependenciesResult
    try {
      dependencies = Dependencies.calcDependencies(plugin, ctx.ide)
    } catch(e: Exception) {
      throw RuntimeException("Unable to evaluate dependencies of the plugin $plugin with IDE ${ctx.ide}", e)
    }

    if (dependencies.cycle != null && ctx.verifierOptions.failOnCyclicDependencies) {
      val cycle = dependencies.cycle.joinToString(separator = " -> ")
      LOG.debug("The plugin verifier will not verify a plugin $ctx.plugin because its dependencies tree has the following cycle: $cycle")
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
      LOG.debug("The plugin verifier will not verify a plugin $plugin because it has missing mandatory dependencies: ${depNode.missingDependencies.entries.joinToString { it.key.toString() }}")
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
          depResolvers.forEach { it.closeLogged() }
          throw ie
        } catch (e: Exception) {
          val isMandatory = mandatoryDeps.find { it.plugin.equals(dep) } != null
          val message = "Unable to read class-files of the ${if (isMandatory) "mandatory" else "optional"} plugin ${dep.pluginId}"

          ctx.registerProblem(MissingDependencyProblem(plugin.pluginId, dep.pluginId, message), ProblemLocation.fromPlugin(plugin.pluginId))

          if (isMandatory) {
            LOG.debug("The plugin verifier will not verify a plugin because its dependent plugin $dep has broken class-files", e)
            depResolvers.forEach { it.closeLogged() }
            return null to VResult.Problems(ctx.pluginDescriptor, ctx.ideDescriptor, "Transitive mandatory dependency $dep is unavailable", ctx.problems)
          } else {
            LOG.debug(message, e)
          }
        }
      }
      return Resolver.createUnionResolver("Plugin ${depNode.plugin} transitive dependencies resolver", depResolvers) to null
    }

  }

  private fun Closeable.closeLogged() {
    try {
      this.close()
    } catch(e: Exception) {
      LOG.warn("Unable to close $this", e)
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

data class VContext(
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
