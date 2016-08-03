package com.jetbrains.pluginverifier.utils.dependencies

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.Plugin
import com.intellij.structure.domain.PluginDependency
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.misc.PluginCache
import com.jetbrains.pluginverifier.repository.RepositoryManager
import org.jgrapht.DirectedGraph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.traverse.DepthFirstIterator
import org.slf4j.LoggerFactory
import java.io.File

fun DirectedGraph<DependencyVertex, DependencyEdge>.getTransitiveDependencies(start: DependencyVertex): List<DependencyVertex> {
  val iterator = DepthFirstIterator(this, start)
  if (!iterator.hasNext()) return emptyList()
  iterator.next() //skip the start
  return Lists.newArrayList(iterator)
}


object Dependencies {

  @JvmStatic
  fun calcDependencies(plugin: Plugin, ide: Ide): DirectedGraph<DependencyVertex, DependencyEdge> {
    val dfs = Dfs(ide)
    dfs.dfs(plugin)
    return dfs.graph
  }

  /**
   * @param ide IDE against which to resolve dependencies.
   */
  private class Dfs(val ide: Ide) {

    val graph: DirectedGraph<DependencyVertex, DependencyEdge> = DefaultDirectedGraph(DependencyEdge::class.java)

    internal fun dfs(plugin: Plugin): DependencyVertex {
      //current node results
      val result = DependencyVertex(plugin)

      if (graph.containsVertex(result)) {
        //either the plugin is visited or it is in-progress
        return result
      }

      graph.addVertex(result)
      val missing = result.missingDependencies

      for (pd in plugin.moduleDependencies + plugin.dependencies) {
        val isModule = plugin.moduleDependencies.indexOf(pd) != -1
        val depId: String = pd.id
        var dependency: Plugin?
        if (isModule) {
          if (isDefaultModule(depId)) {
            continue
          }
          dependency = ide.getPluginByModule(depId)
          if (dependency == null) {
            if (INTELLIJ_MODULES_CONTAINING_PLUGINS.containsKey(depId)) {
              //try to add the intellij plugin which defines this module
              val pluginId = INTELLIJ_MODULES_CONTAINING_PLUGINS[depId]!!
              dependency = ide.getPluginById(pluginId)
              if (dependency == null) {
                try {
                  val updateInfo = RepositoryManager.getInstance().getLastCompatibleUpdateOfPlugin(ide.version, pluginId)
                  if (updateInfo != null) {
                    val pluginFile = RepositoryManager.getInstance().getPluginFile(updateInfo)
                    if (pluginFile != null) {
                      dependency = PluginCache.createPlugin(pluginFile)
                    }
                  }
                } catch (e: Exception) {
                  LOG.debug("Unable to add the plugin " + pluginId + " defining the IntelliJ-module " + depId + " which is required for " + plugin.pluginId, e)
                }
              }
            }

            if (dependency == null) {
              val reason = "Plugin $plugin depends on module $depId which is not found in ${ide.version}"
              missing.put(pd, MissingReason(reason, null))
              continue
            }
          }

        } else {
          dependency = ide.getPluginById(depId)

          if (dependency == null) {
            //try to load plugin
            val updateInfo: UpdateInfo?
            try {
              updateInfo = RepositoryManager.getInstance().getLastCompatibleUpdateOfPlugin(ide.version, depId)
            } catch (e: Exception) {
              val message = "Couldn't get dependency plugin '$depId' from the Plugin Repository for IDE ${ide.version}"
              LOG.debug(message, e)
              missing.put(pd, MissingReason(message, e))
              continue
            }

            if (updateInfo != null) {
              //update does really exist in the repo
              val pluginZip: File?
              try {
                pluginZip = RepositoryManager.getInstance().getPluginFile(updateInfo)
              } catch (e: Exception) {
                val message = "Couldn't download dependency plugin '$depId' from the Plugin Repository for IDE ${ide.version}"
                LOG.debug(message, e)
                missing.put(pd, MissingReason(message, e))
                continue
              }

              if (pluginZip == null) {
                val reason = "The dependency plugin $updateInfo is not found in the Plugin Repository"
                LOG.debug(reason)
                missing.put(pd, MissingReason(reason, null))
                continue
              }

              try {
                dependency = PluginCache.createPlugin(pluginZip)
              } catch (e: Exception) {
                val message = "Plugin $plugin depends on the other plugin $depId which has some problems"
                LOG.debug(message, e)
                missing.put(pd, MissingReason(message, e))
                continue
              }

            }
          }

          if (dependency == null) {
            val message = "Plugin $plugin depends on the other plugin $depId which doesn't have a build compatible with ${ide.version}"
            LOG.debug(message)
            missing.put(pd, MissingReason(message, null))
            continue
          }
        }
        //the dependency is found and is OK.

        //recursively traverse the dependency plugin.
        //it could already be in-progress in which case the cycle is detected.
        val to: DependencyVertex = dfs(dependency)

        graph.addEdge(result, to, DependencyEdge(!pd.isOptional))
      }

      return result
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

class DependencyEdge(val isMandatory: Boolean) : DefaultEdge()

data class DependencyVertex(val plugin: Plugin) {

  val missingDependencies: MutableMap<PluginDependency, MissingReason> = hashMapOf()

  override fun equals(other: Any?): Boolean = other is DependencyVertex && plugin.pluginFile.equals(other.plugin.pluginFile)

  override fun hashCode(): Int = plugin.pluginFile.hashCode()
}

data class MissingReason(val reason: String, val exception: Exception?)