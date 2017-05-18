package com.jetbrains.pluginverifier.utils

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.intellij.structure.ide.Ide
import com.intellij.structure.plugin.Plugin
import com.jetbrains.pluginverifier.dependency.DependencyResolver
import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.repository.FileLock
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

  private fun createDependencyResultByExistingPlugin(plugin: Plugin): DependencyResolver.Result {
    val pluginCreateOk = try {
      PluginCreator.createResolverForExistingPlugin(plugin)
    } catch(e: Throwable) {
      val reason = "Unable to read classes of plugin $plugin"
      LOG.error(reason, e)
      return DependencyResolver.Result.NotFound(reason)
    }
    return DependencyResolver.Result.Found(pluginCreateOk)
  }

  private fun resolvePlugin(dependencyId: String): DependencyResolver.Result {
    val byId = ide.getPluginById(dependencyId)
    if (byId != null) {
      return createDependencyResultByExistingPlugin(byId)
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
    val pluginZip: FileLock = RepositoryManager.getPluginFile(updateInfo)
        ?: return DependencyResolver.Result.NotFound("Plugin $updateInfo is not found in the Plugin Repository")
    return getResultForDependencyByFileLock(pluginZip)
  }

  private fun getResultForDependencyByFileLock(pluginLock: FileLock): DependencyResolver.Result {
    val dependencyCreationResult = PluginCreator.createPluginByFileLock(pluginLock)
    return when (dependencyCreationResult) {
      is CreatePluginResult.OK -> DependencyResolver.Result.Found(dependencyCreationResult)
      is CreatePluginResult.BadPlugin -> DependencyResolver.Result.ProblematicDependency(dependencyCreationResult)
      is CreatePluginResult.NotFound -> DependencyResolver.Result.NotFound(dependencyCreationResult.reason)
    }
  }

  private fun resolveModule(dependencyId: String): DependencyResolver.Result {
    if (isDefaultModule(dependencyId)) {
      return DependencyResolver.Result.Skip
    }
    val byModule = ide.getPluginByModule(dependencyId)
    if (byModule != null) {
      return createDependencyResultByExistingPlugin(byModule)
    }

    if (dependencyId in INTELLIJ_MODULE_TO_CONTAINING_PLUGIN) {
      //try to add the intellij plugin which defines this module
      val pluginId = INTELLIJ_MODULE_TO_CONTAINING_PLUGIN[dependencyId]!!

      val definingPlugin = ide.getPluginById(pluginId)
      if (definingPlugin != null) {
        return createDependencyResultByExistingPlugin(definingPlugin)
      }

      try {
        val updateInfo = RepositoryManager.getLastCompatibleUpdateOfPlugin(ide.version, pluginId)
        if (updateInfo != null) {
          val lock = RepositoryManager.getPluginFile(updateInfo)
          if (lock != null) {
            return getResultForDependencyByFileLock(lock)
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