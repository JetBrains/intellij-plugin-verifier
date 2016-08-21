package com.jetbrains.pluginverifier.api

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.google.common.util.concurrent.MoreExecutors
import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.IdeManager
import com.intellij.structure.domain.IdeVersion
import com.intellij.structure.domain.Plugin
import com.intellij.structure.errors.IncorrectPluginException
import com.intellij.structure.impl.domain.PluginImpl
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyEdge
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.dependencies.MissingReason
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.misc.PluginCache
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.utils.Dependencies
import com.jetbrains.pluginverifier.utils.Edge
import com.jetbrains.pluginverifier.utils.Vertex
import com.jetbrains.pluginverifier.verifiers.VERIFIERS
import com.jetbrains.pluginverifier.warnings.Warning
import org.jgrapht.DirectedGraph
import org.jgrapht.alg.cycle.JohnsonSimpleCycles
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.util.concurrent.*

/**
 * @author Sergey Patrikeev
 */
object VManager {

  private val LOG = LoggerFactory.getLogger(VManager::class.java)

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
                  val statusString = "${result.vResult.pluginDescriptor} has been verified with $ideDescriptor. Result: ${presentableResult(result.vResult)}"
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
      is VResult.Problems -> "It has ${result.problems.keySet().size} problems; " +
          "${result.warnings.size} warnings; " +
          "${result.dependenciesGraph.getMissingNonOptionalDependencies().map { it.missing }.distinct().size} missing dependencies"
      is VResult.BadPlugin -> "It is invalid: ${result.reason}"
      is VResult.NotFound -> "It is not found in the Repository: ${result.reason}"
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
        throw InterruptedException()
      }

      LOG.trace("Verifying $pluginDescriptor with $ideDescriptor")
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

  fun createPlugin(pluginDescriptor: PluginDescriptor, ideVersion: IdeVersion? = null): Pair<Plugin?, VResult?> {
    try {
      return VParamsCreator.getPlugin(pluginDescriptor, ideVersion) to null
    } catch(ie: InterruptedException) {
      throw ie
    } catch(e: IncorrectPluginException) {
      //the plugin has incorrect structure.
      val reason = e.message ?: "The plugin $pluginDescriptor has incorrect structure"
      LOG.debug(reason, e) //this is a problem of the plugin, but not of the Verifier.
      return null to VResult.BadPlugin(pluginDescriptor, reason)
    } catch(e: UpdateNotFoundException) {
      //the caller has specified a missing plugin
      val reason = e.message ?: "The plugin $pluginDescriptor is not found in the Repository"
      LOG.debug(reason, e)
      return null to VResult.NotFound(pluginDescriptor, reason)
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
      val reason = "Unable to read class-files of the plugin"
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
    val (plugin: Plugin?, badResult: VResult?) = createPlugin(pluginDescriptor, ide.version)
    if (badResult != null) {
      return null to badResult
    }
    val (pluginResolver, badResult2) = createPluginResolver(plugin!!, pluginDescriptor)
    if (badResult2 != null) {
      return null to badResult2
    }
    pluginResolver!!.use puse@ {
      val ctx = VContext(plugin, pluginDescriptor, ide, ideDescriptor, params.options)
      val (dependenciesResolver, dependenciesGraph, cycle: List<Plugin>?) = getDependenciesResolver(ctx)

      dependenciesResolver.use {
        try {
          val checkClasses = getClassesForCheck(plugin, pluginResolver)
          val classLoader = createClassLoader(dependenciesResolver, ctx, ideResolver, pluginResolver, runtimeResolver, params.externalClassPath)

          VERIFIERS.forEach { it.verify(ctx, checkClasses, classLoader) }

          val warnings = ctx.warnings +
              (if (plugin is PluginImpl) plugin.hints.map { Warning(it) } else emptyList()) +
              (if (cycle != null) listOf(Warning(cycle.joinToString(separator = " -> ") { it.pluginId })) else emptyList())

          if (ctx.problems.isEmpty) {
            return plugin to VResult.Nice(ctx.pluginDescriptor, ctx.ideDescriptor, warnings)
          } else {
            return plugin to VResult.Problems(ctx.pluginDescriptor, ctx.ideDescriptor, ctx.problems, dependenciesGraph, warnings)
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

  private data class DependenciesResult(val resolver: Resolver,
                                        val dependenciesGraph: DependenciesGraph,
                                        val cycle: List<Plugin>?)


  private fun getDependenciesResolver(ctx: VContext): DependenciesResult {

    val plugin = ctx.plugin
    val graphAndStart: Pair<DirectedGraph<Vertex, Edge>, Vertex>
    try {
      graphAndStart = Dependencies.calcDependencies(plugin, ctx.ide)
    } catch(e: Exception) {
      throw RuntimeException("Unable to evaluate dependencies of the plugin $plugin with IDE ${ctx.ide}", e)
    }
    val (graph, startVertex) = graphAndStart

    var cycle: List<Plugin>? = null
    JohnsonSimpleCycles(graph).findSimpleCycles().forEach {
      //check if the plugin is on the dependencies cycle
      if (startVertex in it) {
        cycle = it.map { it.plugin } + startVertex.plugin
      }
    }

    val resolvers = arrayListOf<Resolver>()
    val vertices = arrayListOf<DependencyNode>()
    val edges = arrayListOf<DependencyEdge>()
    val dfsResult: DfsResult
    try {
      dfsResult = dfs(startVertex, true, graph, hashMapOf(), resolvers, vertices, edges)
    } catch (e: Exception) {
      resolvers.forEach { it.closeLogged() }
      throw RuntimeException("Unable to create dependencies resolver", e)
    }

    val resolver = Resolver.createUnionResolver("Plugin ${startVertex.plugin} transitive dependencies resolver", resolvers)

    val resGraph = DependenciesGraph(dfsResult.vertex!!, vertices, edges)
    return DependenciesResult(resolver, resGraph, cycle)
  }

  private data class DfsResult(val success: Boolean,
                               val missingReason: MissingReason?,
                               val vertex: DependencyNode?)

  private fun dfs(vertex: Vertex,
                  isStart: Boolean,
                  graph: DirectedGraph<Vertex, Edge>,
                  visited: MutableMap<String, DependencyNode>,
                  resolvers: MutableList<Resolver>,
                  vertices: MutableList<DependencyNode>,
                  edges: MutableList<DependencyEdge>): DfsResult {
    if (!isStart) {
      try {
        val resolver = Resolver.createPluginResolver(vertex.plugin)
        resolvers.add(resolver)
      } catch (e: Exception) {
        return DfsResult(false, MissingReason("Failed to read the class-files of the plugin ${vertex.plugin}"), null)
      }
    }

    val missingDependencies = vertex.missingDependencies
    val node = DependencyNode(vertex.plugin.pluginId ?: "", vertex.plugin.pluginVersion ?: "", missingDependencies)
    vertices.add(node)

    visited[vertex.plugin.pluginId] = node

    graph.outgoingEdgesOf(vertex).forEach {
      if (it.to.plugin.pluginId !in visited) {
        val (success, missingReason, toNode) = dfs(it.to, false, graph, visited, resolvers, vertices, edges)
        if (!success) {
          missingDependencies[it.dependency] = missingReason!!
        } else {
          edges.add(DependencyEdge(node, toNode!!, it.dependency))
        }
      } else {
        edges.add(DependencyEdge(node, visited[it.to.plugin.pluginId]!!, it.dependency))
      }
    }

    return DfsResult(true, null, node)
  }


  private fun Closeable.closeLogged() {
    try {
      this.close()
    } catch(e: Exception) {
      LOG.warn("Unable to close $this", e)
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

  val warnings: MutableList<Warning> = arrayListOf()

  fun registerProblem(problem: Problem, location: ProblemLocation) {
    if (!verifierOptions.isIgnoredProblem(plugin, problem)) {
      problems.put(problem, location)
    }
  }

  fun registerWarning(warning: Warning) {
    warnings.add(warning)
  }

}

private object VParamsCreator {

  /**
   * Creates the [Resolver] by the given JDK descriptor.
   *
   * @throws IOException if the [Resolver] cannot be created.
   * @return [Resolver] of the JDK classes
   */
  @Throws(IOException::class)
  fun createJdkResolver(jdkDescriptor: JdkDescriptor): Resolver = when (jdkDescriptor) {
    is JdkDescriptor.ByFile -> Resolver.createJdkResolver(jdkDescriptor.file)
  }

  /**
   * Creates the Plugin instance by the given Plugin descriptor.
   * If the descriptor specifies the plugin build id, it firstly loads the
   * corresponding plugin build from the Repository.
   *
   * @param ideVersion the version of the compatible IDE. It's used if the plugin descriptor specifies the plugin id only.
   * @throws IncorrectPluginException if the specified plugin has incorrect structure
   * @throws IOException if the plugin has a broken File.
   * @throws UpdateNotFoundException if the plugin is not found in the Repository
   * @throws RepositoryDoesntRespondException if the Repository doesn't respond
   */
  @Throws(IncorrectPluginException::class, IOException::class, UpdateNotFoundException::class, RuntimeException::class)
  fun getPlugin(plugin: PluginDescriptor, ideVersion: IdeVersion?): Plugin = when (plugin) {
    is PluginDescriptor.ByInstance -> plugin.plugin //already created.
    is PluginDescriptor.ByFile -> PluginCache.createPlugin(plugin.file) //IncorrectPluginException, IOException
    is PluginDescriptor.ByXmlId -> {
      val updates = withConnectionCheck { RepositoryManager.getInstance().getAllCompatibleUpdatesOfPlugin(ideVersion!!, plugin.pluginId) }
      val suitable: UpdateInfo = updates.find { plugin.version.equals(it.version) } ?: throw noSuchUpdate(plugin)
      val file: File
      try {
        file = RepositoryManager.getInstance().getPluginFile(suitable) ?: throw noSuchUpdate(plugin)
      } catch (e: Exception) {
        throw noSuchUpdate(plugin, e)
      }
      PluginCache.createPlugin(file) //IncorrectPluginException, IOException
    }
    is PluginDescriptor.ByUpdateInfo -> {
      val file = withConnectionCheck { RepositoryManager.getInstance().getPluginFile(plugin.updateInfo) } ?: throw noSuchUpdate(plugin)
      PluginCache.createPlugin(file) //IncorrectPluginException, IOException
    }
  }

  private fun <T> withConnectionCheck(block: () -> T): T {
    try {
      return block()
    } catch (ie: InterruptedException) {
      throw ie
    } catch(e: Exception) {
      throw RepositoryDoesntRespondException(e.message ?: e.javaClass.name, e)
    }
  }

  private fun noSuchUpdate(plugin: PluginDescriptor, exception: Exception? = null): UpdateNotFoundException {
    return UpdateNotFoundException("Plugin $plugin is not found in the Plugin repository${if (exception != null) exception.message ?: exception.javaClass.name else ""}")
  }

  fun getIde(ideDescriptor: IdeDescriptor): Ide = when (ideDescriptor) {
    is IdeDescriptor.ByFile -> IdeManager.getInstance().createIde(ideDescriptor.file)
    is IdeDescriptor.ByInstance -> ideDescriptor.ide
    is IdeDescriptor.ByVersion -> TODO("Downloading the IDE by IdeVersion is not supported yet.")
    IdeDescriptor.AnyIde -> throw IllegalArgumentException()
  }


}

/**
 * The exception signals that the plugin is not found in the Repository.
 */
private class UpdateNotFoundException(message: String, cause: Exception? = null) : RuntimeException(message, cause)

/**
 * The exception signals that the Plugin repository is not available now
 */
private class RepositoryDoesntRespondException(message: String, cause: Exception? = null) : RuntimeException(message, cause)