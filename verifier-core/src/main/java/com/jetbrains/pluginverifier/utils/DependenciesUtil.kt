package com.jetbrains.pluginverifier.utils

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.Plugin
import com.intellij.structure.domain.PluginDependency
import com.jetbrains.pluginverifier.api.DependencyResolver
import com.jetbrains.pluginverifier.api.DependencyResolver.Result
import com.jetbrains.pluginverifier.dependencies.MissingReason
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.misc.PluginCache
import com.jetbrains.pluginverifier.repository.IFileLock
import com.jetbrains.pluginverifier.repository.RepositoryManager
import org.jgrapht.DirectedGraph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.slf4j.LoggerFactory

class DefaultDependencyResolver(val ide: Ide) : DependencyResolver {

  override fun resolve(dependencyId: String, isModule: Boolean, dependent: Plugin): DependencyResolver.Result {

    if (isModule) {
      if (isDefaultModule(dependencyId)) {
        return Result.Skip
      }
      val byModule = ide.getPluginByModule(dependencyId)
      if (byModule != null) {
        return Result.Found(byModule)
      }

      if (INTELLIJ_MODULES_CONTAINING_PLUGINS.containsKey(dependencyId)) {
        //try to add the intellij plugin which defines this module
        val pluginId = INTELLIJ_MODULES_CONTAINING_PLUGINS[dependencyId]!!

        val definingPlugin = ide.getPluginById(pluginId)
        if (definingPlugin != null) {
          return Result.Found(definingPlugin)
        }

        try {
          val updateInfo = RepositoryManager.getLastCompatibleUpdateOfPlugin(ide.version, pluginId)
          if (updateInfo != null) {
            val lock = RepositoryManager.getPluginFile(updateInfo)
            if (lock != null) {
              try {
                val dependency = PluginCache.createPlugin(lock.getFile())
                return Result.Created(dependency, lock)
              } catch (e: Exception) {
                lock.release()
                throw e
              }
            }
          }
        } catch (e: Exception) {
          LOG.debug("Unable to add the dependent " + pluginId + " defining the IntelliJ-module " + dependencyId + " which is required for " + dependent.pluginId, e)
        }
      }

      val reason = MissingReason("Plugin $dependent depends on module $dependencyId which is not found in ${ide.version}")
      return Result.NotFound(reason)
    } else {
      val byId = ide.getPluginById(dependencyId)
      if (byId != null) {
        return Result.Found(byId)
      }

      //try to load plugin
      val updateInfo: UpdateInfo?
      try {
        updateInfo = RepositoryManager.getLastCompatibleUpdateOfPlugin(ide.version, dependencyId)
      } catch (e: Exception) {
        val message = "Couldn't get dependency plugin '$dependencyId' from the Plugin Repository for IDE ${ide.version}"
        LOG.debug(message, e)
        return Result.NotFound(MissingReason(message))
      }

      if (updateInfo != null) {
        //update does really exist in the repo
        val pluginZip: IFileLock?
        try {
          pluginZip = RepositoryManager.getPluginFile(updateInfo)
        } catch (e: Exception) {
          val message = "Couldn't download dependency plugin '$dependencyId' from the Plugin Repository for IDE ${ide.version}"
          LOG.debug(message, e)
          return Result.NotFound(MissingReason(message))
        }

        if (pluginZip == null) {
          val reason = "The dependency plugin $updateInfo is not found in the Plugin Repository"
          LOG.debug(reason)
          return Result.NotFound(MissingReason(reason))
        }

        try {
          val dependency = PluginCache.createPlugin(pluginZip.getFile())
          return Result.Created(dependency, pluginZip)
        } catch (e: Exception) {
          pluginZip.release()
          val message = "Plugin $dependent depends on the other plugin $dependencyId which has some problems"
          LOG.debug(message, e)
          return Result.NotFound(MissingReason(message))
        }
      }

      val message = "Plugin $dependent depends on the other plugin $dependencyId which doesn't have a build compatible with ${ide.version}"
      LOG.debug(message)
      return Result.NotFound(MissingReason(message))
    }
  }

  private val LOG = LoggerFactory.getLogger(Dependencies::class.java)

  /**
   * The list of IntelliJ plugins which define some modules
   * (e.g. the plugin "org.jetbrains.plugins.ruby" defines a module "com.intellij.modules.ruby")
   */
  private val INTELLIJ_MODULES_CONTAINING_PLUGINS = ImmutableMap.of(
      "com.intellij.modules.ruby", "org.jetbrains.plugins.ruby",
      "com.intellij.modules.php", "com.jetbrains.php",
      "com.intellij.modules.python", "Pythonid",
      "com.intellij.modules.swift.lang", "com.intellij.clion-swift")

  private val IDEA_ULTIMATE_MODULES = ImmutableList.of(
      "com.intellij.modules.platform",
      "com.intellij.modules.lang",
      "com.intellij.modules.vcs",
      "com.intellij.modules.xml",
      "com.intellij.modules.xdebugger",
      "com.intellij.modules.java",
      "com.intellij.modules.ultimate",
      "com.intellij.modules.all")

  private fun isDefaultModule(moduleId: String): Boolean = IDEA_ULTIMATE_MODULES.contains(moduleId)


}

object Dependencies {

  data class Result(val graph: DirectedGraph<Vertex, Edge>, val start: Vertex, val allLocks: List<IFileLock>)

  @JvmStatic
  fun calcDependencies(plugin: Plugin, resolver: DependencyResolver): Result {
    val dfs = Dfs(resolver)
    try {
      val vertex = dfs.dfs(plugin)
      return Result(dfs.graph, vertex, dfs.allLocks)
    } catch (e: Exception) {
      dfs.allLocks.forEach { it.release() }
      throw e
    }
  }

  private class Dfs(val resolver: DependencyResolver) {

    val graph: DirectedGraph<Vertex, Edge> = DefaultDirectedGraph(Edge::class.java)

    val allLocks: MutableList<IFileLock> = arrayListOf()

    internal fun dfs(plugin: Plugin): Vertex {
      //current node results
      val result = Vertex(plugin)

      if (graph.containsVertex(result)) {
        //either the plugin is visited or it is in-progress
        return result
      }

      graph.addVertex(result)
      val missing = result.missingDependencies

      forik@ for (pd in plugin.moduleDependencies + plugin.dependencies) {
        val isModule = pd in plugin.moduleDependencies
        var dependency: Plugin?

        //find in already visited plugins
        dependency = graph.vertexSet().find { pd.id == it.plugin.pluginId }?.plugin
        if (dependency == null) {
          val resolution = resolver.resolve(pd.id, isModule, plugin)
          dependency = when (resolution) {
            is DependencyResolver.Result.Found -> {
              resolution.plugin
            }
            is DependencyResolver.Result.Created -> {
              allLocks.add(resolution.fileLock)
              resolution.plugin
            }
            is DependencyResolver.Result.NotFound -> {
              missing.put(pd, resolution.reason)
              continue@forik
            }
            DependencyResolver.Result.Skip -> {
              continue@forik
            }
          }
        }
        //the dependency is found and is OK.

        //recursively traverse the dependency plugin.
        //it could already be in-progress in which case the cycle is detected.
        val to: Vertex = dfs(dependency)

        graph.addEdge(result, to, Edge(pd, result, to))
      }

      return result
    }

  }


}

class Edge(val dependency: PluginDependency, val from: Vertex, val to: Vertex) : DefaultEdge() {
  override fun equals(other: Any?): Boolean = other is Edge && other.dependency == dependency && other.from == from && other.to == to

  override fun hashCode(): Int = dependency.hashCode() + from.hashCode() + to.hashCode()
}

data class Vertex(val plugin: Plugin) {

  val missingDependencies: MutableMap<PluginDependency, MissingReason> = hashMapOf()

}