package com.jetbrains.pluginverifier.api

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.google.common.util.concurrent.MoreExecutors
import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.IdeVersion
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
import java.util.concurrent.*

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

  private fun getWorkersNumber() = Math.max(8, Runtime.getRuntime().availableProcessors())

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

        var ide: Ide = createIde(ideDescriptor)
        val (closeIdeResolver: Boolean, ideResolver: Resolver) = ideResolverPair(ide, ideDescriptor)

        try {
          val executor = if (params.resolveDependenciesWithin) MoreExecutors.newDirectExecutorService() else Executors.newFixedThreadPool(getWorkersNumber())
          val ecp = ExecutorCompletionService<VCallableResult>(executor)
          val futures = ideToPlugins.value.map { it.first }.map { ecp.submit(VCallable(it, ideDescriptor, ide, params, ideResolver, runtimeResolver)) }
          val totalN = futures.size
          try {
            (0..totalN - 1).forEach fori@ {
              while (true) {
                if (Thread.currentThread().isInterrupted) {
                  throw InterruptedException()
                }
                val future = ecp.poll(500, TimeUnit.MILLISECONDS) //throws InterruptedException (it's ok)
                if (future != null) {

                  val result: VCallableResult
                  try {
                    result = future.get()
                  } catch (ie: InterruptedException) {
                    throw ie
                  } catch (e: CancellationException) {
                    throw InterruptedException()
                  } catch (e: ExecutionException) {
                    throw e.cause ?: e
                  } catch (e: Exception) {
                    throw RuntimeException("Unexpected exception in the task", e)
                  }

                  results.add(result.vResult)

                  if (params.resolveDependenciesWithin && result.plugin != null) {
                    ide = ide.getExpandedIde(result.plugin)
                  }

                  progress.setProgress(((++verified).toDouble()) / pluginsNumber)
                  val statusString = "${result.vResult.pluginDescriptor.presentableName()} has been verified with ${ideDescriptor.presentableName()}. Result: ${presentableResult(result.vResult)}"
                  progress.setText(statusString)
                  LOG.trace("$statusString; progress = $verified out of $pluginsNumber")

                  break
                }
              }
            }
          } finally {
            futures.forEach { it.cancel(true) }
            executor.shutdownNow()
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

  private fun presentableResult(result: VResult): String {
    val resType: String = when (result) {
      is VResult.Nice -> "It is OK."
      is VResult.Problems -> "It has ${result.problems.keySet().size} problems"
      is VResult.BadPlugin -> "It is invalid: ${result.overview}"
      is VResult.NotFound -> "It is not found in the Repository: ${result.overview}"
    }
    return resType
  }

  private data class VCallableResult(val plugin: Plugin?, val vResult: VResult)

  private class VCallable(val pluginDescriptor: PluginDescriptor,
                          val ideDescriptor: IdeDescriptor,
                          val ide: Ide,
                          val params: VParams,
                          val ideResolver: Resolver,
                          val runtimeResolver: Resolver) : Callable<VCallableResult> {
    override fun call(): VCallableResult {
      if (Thread.currentThread().isInterrupted) {
        throw InterruptedException("The verification was cancelled")
      }

      LOG.trace("Verifying ${pluginDescriptor.presentableName()} with ${ideDescriptor.presentableName()}")
      val (plugin, vResult) = verification(pluginDescriptor, ide, ideDescriptor, params, ideResolver, runtimeResolver)
      return VCallableResult(plugin, vResult)
    }

  }


  private fun createIde(ideDescriptor: IdeDescriptor): Ide {
    try {
      LOG.trace("Creating IDE instance for $ideDescriptor")
      return VParamsCreator.getIde(ideDescriptor)
    } catch(ie: InterruptedException) {
      throw ie
    } catch(e: Exception) {
      //IDE errors are propagated. We assume the IDE-s are correct while the plugins may not be so.
      throw RuntimeException("Failed to create IDE instance for $ideDescriptor")
    }
  }

  private fun createPlugin(pluginDescriptor: PluginDescriptor, ideDescriptor: IdeDescriptor, ideVersion: IdeVersion): Pair<Plugin?, VResult?> {
    try {
      return VParamsCreator.getPlugin(pluginDescriptor, ideVersion) to null
    } catch(ie: InterruptedException) {
      throw ie
    } catch(e: IncorrectPluginException) {
      //the plugin has incorrect structure.
      val reason = e.message ?: "The plugin ${pluginDescriptor.presentableName()} has incorrect structure"
      LOG.debug(reason, e) //this is a problem of the plugin, but not of the Verifier.
      return null to VResult.BadPlugin(pluginDescriptor, reason)
    } catch(e: UpdateNotFoundException) {
      //the caller has specified a missing plugin
      val reason = e.message ?: "The plugin ${pluginDescriptor.presentableName()} is not found in the Repository"
      LOG.debug(reason, e)
      return null to VResult.NotFound(pluginDescriptor, ideDescriptor, reason)
    } catch(e: IOException) {
      //the plugin has an invalid file
      val reason = e.message ?: e.javaClass.name
      LOG.debug(reason, e)
      return null to VResult.BadPlugin(pluginDescriptor, reason)
    } catch (e: RuntimeException) {
      throw e
    } catch (e: Exception) {
      throw RuntimeException(e)
    }
  }

  private fun createPluginResolver(plugin: Plugin, pluginDescriptor: PluginDescriptor): Pair<Resolver?, VResult?> {
    try {
      return Resolver.createPluginResolver(plugin) to null
    } catch (ie: InterruptedException) {
      throw ie
    } catch(e: Exception) {
      val reason = e.message ?: "Failed to read the class-files of the plugin $plugin"
      LOG.debug(reason, e)
      return null to VResult.BadPlugin(pluginDescriptor, reason)
    }
  }

  private fun verification(pluginDescriptor: PluginDescriptor,
                           ide: Ide,
                           ideDescriptor: IdeDescriptor,
                           params: VParams,
                           ideResolver: Resolver,
                           runtimeResolver: Resolver): Pair<Plugin?, VResult> {
    val (plugin: Plugin?, badResult: VResult?) = createPlugin(pluginDescriptor, ideDescriptor, ide.version)
    if (badResult != null) {
      return null to badResult
    }
    val (pluginResolver, badResult2) = createPluginResolver(plugin!!, pluginDescriptor)
    if (badResult2 != null) {
      return null to badResult2
    }
    pluginResolver!!.use puse@ {
      val ctx = VContext(plugin, pluginDescriptor, ide, ideDescriptor, params.options)
      val (dependenciesResolver: Resolver?, vResult: VResult?) = getDependenciesResolver(ctx)
      if (vResult != null) {
        return plugin to vResult
      }

      dependenciesResolver!!.use {
        try {
          val checkClasses = getClassesForCheck(plugin, pluginResolver)
          val classLoader = createClassLoader(dependenciesResolver, ctx, ideResolver, pluginResolver, runtimeResolver, params.externalClassPath)

          ReferencesVerifier.verify(ctx, checkClasses, classLoader)

          if (ctx.problems.isEmpty) {
            return plugin to VResult.Nice(ctx.pluginDescriptor, ctx.ideDescriptor, plugin.hints.joinToString())
          } else {
            return plugin to VResult.Problems(ctx.pluginDescriptor, ctx.ideDescriptor, plugin.hints.joinToString(), ctx.problems)
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
  }

  private fun ideResolverPair(ide: Ide, ideDescriptor: IdeDescriptor): Pair<Boolean, Resolver> {
    //we must not close the IDE Resolver coming from the caller
    LOG.trace("Creating IDE Resolver instance for $ide")
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
