package com.jetbrains.pluginverifier.utils.dependencies

import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.Plugin
import com.intellij.structure.domain.PluginDependency
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.misc.PluginCache
import com.jetbrains.pluginverifier.repository.RepositoryManager
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

object Dependencies {

  @JvmStatic
  fun calcDependencies(plugin: Plugin, ide: Ide): DependenciesResult {
    val dfs = Dfs(ide)
    val result = dfs.dfs(plugin)
    Preconditions.checkNotNull(result)
    return DependenciesResult(result, dfs.cycle)
  }

  private class Dfs internal constructor(
      /**
       * IDE against which to resolve dependencies.
       */
      internal val ide: Ide) {

    /**
     * Already calculated plugin nodes.
     */
    internal val nodes: MutableMap<Plugin, PluginDependenciesNode> = hashMapOf()

    /**
     * Current DFS state.
     */
    internal val state: MutableMap<Plugin, DfsState> = hashMapOf()

    /**
     * DFS path.
     */
    internal val path: MutableList<Plugin> = ArrayList()

    /**
     * This is some cycle in the dependencies graph, or null if no cycle
     */
    internal var cycle: MutableList<Plugin>? = null

    internal fun dfs(plugin: Plugin): PluginDependenciesNode {
      if (nodes.containsKey(plugin)) {
        //already calculated.
        Preconditions.checkArgument(state.containsKey(plugin) && state[plugin] == DfsState.BLACK)
        return nodes[plugin]!!
      }

      //assure plugin is not in-progress.
      Preconditions.checkArgument(!state.containsKey(plugin))

      //mark as in-progress
      state.put(plugin, DfsState.GRAY)

      path.add(plugin)

      //current node results
      val transitives = hashSetOf<Plugin>()
      val missing = hashMapOf<PluginDependency, MissingReason>()
      val edges = hashSetOf<PluginDependenciesNode>()

      try {
        //process plugin dependencies.

        val union = ArrayList(plugin.moduleDependencies)
        union.addAll(plugin.dependencies)

        for (pd in union) {
          val isModule = plugin.moduleDependencies.indexOf(pd) != -1
          val depId = pd.id
          var dependency: Plugin?
          if (isModule) {
            if (isDefaultModule(depId)) {
              continue
            }
            dependency = ide.getPluginByModule(depId)
            if (dependency == null) {
              if (INTELLIJ_MODULES_CONTAINING_PLUGINS.containsKey(depId)) {
                //try to add the intellij plugin which defines this module
                val pluginId = INTELLIJ_MODULES_CONTAINING_PLUGINS[depId]
                if (pluginId != null) {
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
                      LOG.error("Unable to add the plugin " + pluginId + " defining the IntelliJ-module " + depId + " which is required for " + plugin.pluginId, e)
                    }
                  }
                }
              }

              if (dependency == null) {
                val reason = String.format("Plugin %s depends on module %s which is not found in %s", plugin.pluginId, depId, ide.version)
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
                val message = String.format("Couldn't get dependency plugin '%s' from the Plugin Repository for IDE %s", depId, ide.version)
                LOG.error(message, e)
                missing.put(pd, MissingReason(message, e))
                continue
              }

              if (updateInfo != null) {
                //update does really exist in the repo
                val pluginZip: File?
                try {
                  pluginZip = RepositoryManager.getInstance().getPluginFile(updateInfo)
                } catch (e: Exception) {
                  val message = String.format("Couldn't download dependency plugin '%s' from the Plugin Repository for IDE %s", depId, ide.version)
                  LOG.error(message, e)
                  missing.put(pd, MissingReason(message, e))
                  continue
                }

                if (pluginZip == null) {
                  val reason = "The dependency plugin $updateInfo is not found in the Plugin Repository"
                  LOG.error(reason)
                  missing.put(pd, MissingReason(reason, null))
                  continue
                }

                try {
                  dependency = PluginCache.createPlugin(pluginZip)
                } catch (e: Exception) {
                  val message = String.format("Plugin %s depends on the other plugin %s which has some problems%s", plugin, depId, if (e.message != null) e.message else "")
                  LOG.error(message, e)
                  missing.put(pd, MissingReason(message, e))
                  continue
                }

              }
            }

            if (dependency == null) {
              val message = String.format("Plugin %s depends on the other plugin %s which doesn't have a build compatible with %s", plugin, depId, ide.version)
              LOG.error(message)
              missing.put(pd, MissingReason(message, null))
              continue
            }
          }
          //the dependency is found and is OK.


          //check if cycle
          if (state.containsKey(dependency) && state[dependency] == DfsState.GRAY) {
            val idx = path.lastIndexOf(dependency)
            Preconditions.checkArgument(idx != -1)
            cycle = ArrayList(path.subList(idx, path.size))
            cycle!!.add(dependency) //first and last entries are the same (A -> B -> C -> A)

            //TODO: we can't append edges and transitives at this point, because the dependency is currently calculating.
            continue
          }

          val to = dfs(dependency)
          edges.add(to)
          transitives.add(to.plugin) //the dependency itself
          transitives.addAll(to.transitiveDependencies) //and all its transitive dependencies
        }

        val result = PluginDependenciesNode(plugin, edges, transitives, missing)

        //remember the result.
        nodes.put(plugin, result)

        return result

      } finally {
        //plugin is visited
        state.put(plugin, DfsState.BLACK)

        val lastIdx = path.size - 1
        Preconditions.checkArgument(path.size > 0 && path[lastIdx] === plugin)
        path.removeAt(lastIdx)
      }
    }

    private enum class DfsState {
      GRAY, //in progress
      BLACK //already visited
    }

  }

  class DependenciesResult(val descriptor: PluginDependenciesNode,
                           /**
                            * Not-null value represents some cycle in the dependencies graph.
                            * It's for the caller consideration whether to throw an exception in such a case.
                            */
                           val cycle: List<Plugin>?)


  private val LOG = LoggerFactory.getLogger(Dependencies::class.java)

  /**
   * The list of IntelliJ plugins which define some modules
   * (e.g. the plugin "org.jetbrains.plugins.ruby" defines a module "com.intellij.modules.ruby")
   */
  //TODO: add a cli-option
  private val INTELLIJ_MODULES_CONTAINING_PLUGINS = ImmutableMap.of(
      "com.intellij.modules.ruby", "org.jetbrains.plugins.ruby",
      "com.intellij.modules.php", "com.jetbrains.php",
      "com.intellij.modules.python", "Pythonid",
      "com.intellij.modules.swift.lang", "com.intellij.clion-swift")

  //TODO: write a System.option for appending this list.
  private val IDEA_ULTIMATE_MODULES = ImmutableList.of(
      "com.intellij.modules.platform",
      "com.intellij.modules.lang",
      "com.intellij.modules.vcs",
      "com.intellij.modules.xml",
      "com.intellij.modules.xdebugger",
      "com.intellij.modules.java",
      "com.intellij.modules.ultimate",
      "com.intellij.modules.all")
  //TODO: add some caching

  private fun isDefaultModule(moduleId: String): Boolean {
    return IDEA_ULTIMATE_MODULES.contains(moduleId)
  }


}
