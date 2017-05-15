package com.jetbrains.pluginverifier.utils

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.Plugin
import com.intellij.structure.domain.PluginManager
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.DependencyResolver
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.repository.FileLock
import com.jetbrains.pluginverifier.repository.IdleFileLock
import com.jetbrains.pluginverifier.repository.RepositoryManager
import org.slf4j.LoggerFactory

class DefaultDependencyResolver(val ide: Ide) : DependencyResolver {

  companion object {
    private val LOG = LoggerFactory.getLogger(DefaultDependencyResolver::class.java)

    /**
     * The list of IntelliJ plugins which define some modules
     * (e.g. the plugin "org.jetbrains.plugins.ruby" defines a module "com.intellij.modules.ruby")
     */
    private val INTELLIJ_MODULE_TO_CONTAINING_PLUGIN = ImmutableMap.of(
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

    private fun isDefaultModule(moduleId: String): Boolean = moduleId in IDEA_ULTIMATE_MODULES

  }

  override fun resolve(dependencyId: String, isModule: Boolean, dependent: Plugin): DependencyResolver.Result {
    if (isModule) {
      return resolveModule(dependencyId)
    } else {
      return resolvePlugin(dependencyId)
    }
  }

  private fun tryCreatePluginResolver(plugin: Plugin, fileLock: FileLock): DependencyResolver.Result {
    try {
      val resolver = Resolver.createPluginResolver(plugin)
      return DependencyResolver.Result.Found(plugin, resolver, fileLock)
    } catch(e: Throwable) {
      fileLock.release()
      val reason = "Unable to read classes of plugin $plugin"
      LOG.error(reason, e)
      return DependencyResolver.Result.NotFound(reason)
    }
  }

  private fun resolvePlugin(dependencyId: String): DependencyResolver.Result {
    val byId = ide.getPluginById(dependencyId)
    if (byId != null) {
      return tryCreatePluginResolver(byId, IdleFileLock(byId.pluginFile))
    }

    //try to load plugin
    val updateInfo: UpdateInfo? = try {
      RepositoryManager.getLastCompatibleUpdateOfPlugin(ide.version, dependencyId)
    } catch (e: Exception) {
      val message = "Unable to download plugin $dependencyId from the Plugin Repository"
      LOG.info(message, e)
      return DependencyResolver.Result.NotFound(message)
    }

    if (updateInfo != null) {
      //update does really exist in the repo
      return downloadAndOpenPlugin(updateInfo)
    }

    val message = "Plugin $dependencyId doesn't have a build compatible with ${ide.version}"
    LOG.debug(message)
    return DependencyResolver.Result.NotFound(message)
  }

  private fun downloadAndOpenPlugin(updateInfo: UpdateInfo): DependencyResolver.Result {
    val pluginZip: FileLock? = try {
      RepositoryManager.getPluginFile(updateInfo)
    } catch (e: Exception) {
      val message = "Unable to download plugin $updateInfo from the Plugin Repository"
      LOG.info(message, e)
      return DependencyResolver.Result.NotFound(message)
    }

    if (pluginZip == null) {
      val reason = "Plugin $updateInfo is not found in the Plugin Repository"
      LOG.info(reason)
      return DependencyResolver.Result.NotFound(reason)
    }

    val dependency = try {
      PluginManager.getInstance().createPlugin(pluginZip.getFile())
    } catch (e: Throwable) {
      pluginZip.release()
      val message = "Plugin $updateInfo is invalid"
      LOG.info(message, e)
      return DependencyResolver.Result.NotFound(message)
    }

    return tryCreatePluginResolver(dependency, pluginZip)
  }

  private fun resolveModule(dependencyId: String): DependencyResolver.Result {
    if (isDefaultModule(dependencyId)) {
      return DependencyResolver.Result.Skip
    }
    val byModule = ide.getPluginByModule(dependencyId)
    if (byModule != null) {
      return tryCreatePluginResolver(byModule, IdleFileLock(byModule.pluginFile))
    }

    if (dependencyId in INTELLIJ_MODULE_TO_CONTAINING_PLUGIN) {
      //try to add the intellij plugin which defines this module
      val pluginId = INTELLIJ_MODULE_TO_CONTAINING_PLUGIN[dependencyId]!!

      val definingPlugin = ide.getPluginById(pluginId)
      if (definingPlugin != null) {
        return tryCreatePluginResolver(definingPlugin, IdleFileLock(definingPlugin.pluginFile))
      }

      try {
        val updateInfo = RepositoryManager.getLastCompatibleUpdateOfPlugin(ide.version, pluginId)
        if (updateInfo != null) {
          val lock = RepositoryManager.getPluginFile(updateInfo)
          if (lock != null) {
            val dependency = try {
              PluginManager.getInstance().createPlugin(lock.getFile())
            } catch (e: Throwable) {
              lock.release()
              throw e
            }
            return tryCreatePluginResolver(dependency, lock)
          }
        }
      } catch (e: Throwable) {
        LOG.error("Unable to add the dependent $pluginId defining the IntelliJ-module $dependencyId", e)
      }
    }

    val reason = "Module $dependencyId is not found in ${ide.version}"
    return DependencyResolver.Result.NotFound(reason)
  }

}