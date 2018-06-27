package com.jetbrains.pluginverifier.options

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.exists
import com.jetbrains.pluginverifier.misc.readLines
import com.jetbrains.pluginverifier.misc.tryInvokeSeveralTimes
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportage
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import com.jetbrains.pluginverifier.tasks.InvalidPluginFile
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class PluginsParsing(private val pluginRepository: PluginRepository,
                     private val verificationReportage: VerificationReportage) {

  fun parsePluginsToCheck(pluginToTestArg: String, ideVersions: List<IdeVersion>): PluginsSet {
    verificationReportage.logVerificationStage("Parse a list of plugins to check")
    val pluginsSet = PluginsSet()
    when {
      pluginToTestArg.startsWith("@") -> schedulePluginsFromFile(
          pluginsSet,
          Paths.get(pluginToTestArg.substringAfter("@")),
          ideVersions
      )
      pluginToTestArg.matches("#\\d+".toRegex()) -> {
        val updateId = Integer.parseInt(pluginToTestArg.drop(1))
        val updateInfo = pluginRepository.tryInvokeSeveralTimes(3, 5, TimeUnit.SECONDS, "get update information for update #$updateId") {
          (this as? MarketplaceRepository)?.getPluginInfoById(updateId)
        } ?: throw IllegalArgumentException("Update #$updateId is not found in the Plugin Repository")
        pluginsSet.schedulePlugin(updateInfo)
      }
      else -> addPluginToCheckFromFile(Paths.get(pluginToTestArg), pluginsSet)
    }
    return pluginsSet
  }

  /**
   * Parses the command line for a set of plugins to be checked
   * and requests [UpdateInfo]s compatible with [ideVersion].
   */
  fun parsePluginsToCheck(opts: CmdOpts, ideVersion: IdeVersion): PluginsSet {
    val (allVersions, lastVersions) = parseAllAndLastPluginIdsToCheck(opts)

    val pluginsSet = PluginsSet()
    tryInvokeSeveralTimes(3, 5, TimeUnit.SECONDS, "fetch updates to check against $ideVersion") {
      val pluginInfos = requestUpdatesToCheckByIds(allVersions, lastVersions, ideVersion)
      pluginsSet.schedulePlugins(pluginInfos)
    }
    return pluginsSet
  }

  private fun schedulePluginsFromFile(pluginsSet: PluginsSet,
                                      pluginsListFile: Path,
                                      ideVersions: List<IdeVersion>) {
    val pluginPaths = pluginsListFile.readLines().map { it.trim() }.filterNot { it.isEmpty() }
    for (ideVersion in ideVersions) {
      for (path in pluginPaths) {
        if (path.startsWith("id:")) {
          val compatiblePluginVersions = getCompatiblePluginVersions(path.substringAfter("id:"), ideVersion)
          pluginsSet.schedulePlugins(compatiblePluginVersions)
        } else if (path.startsWith("#")) {
          val updateId = path.substringAfter("#").toIntOrNull() ?: continue
          val pluginInfo = getPluginInfoByUpdateId(updateId) ?: continue
          pluginsSet.schedulePlugin(pluginInfo)
        } else {
          var pluginFile = Paths.get(path)
          if (!pluginFile.isAbsolute) {
            pluginFile = pluginsListFile.resolveSibling(path)
          }
          if (!pluginFile.exists()) {
            throw RuntimeException("Plugin file '$path' with absolute '${pluginFile.toAbsolutePath()}' specified in '${pluginsListFile.toAbsolutePath()}' doesn't exist")
          }
          verificationReportage.logVerificationStage("Reading descriptor of a plugin to check from $pluginFile")
          addPluginToCheckFromFile(pluginFile, pluginsSet)
        }
      }
    }
  }

  private fun addPluginToCheckFromFile(pluginFile: Path, pluginsSet: PluginsSet) {
    with(IdePluginManager.createManager().createPlugin(pluginFile.toFile())) {
      when (this) {
        is PluginCreationSuccess -> pluginsSet.scheduleLocalPlugin(plugin)
        is PluginCreationFail -> {
          verificationReportage.logVerificationStage("Plugin is invalid in $pluginFile: ${errorsAndWarnings.joinToString()}")
          pluginsSet.invalidPluginFiles.add(InvalidPluginFile(pluginFile, errorsAndWarnings))
        }
      }
    }
  }

  private fun getPluginInfoByUpdateId(updateId: Int): PluginInfo? =
      pluginRepository.tryInvokeSeveralTimes(3, 5, TimeUnit.SECONDS, "fetch plugin info for #$updateId") {
        (pluginRepository as? MarketplaceRepository)?.getPluginInfoById(updateId)
      }

  private fun getCompatiblePluginVersions(pluginId: String, ideVersion: IdeVersion): List<PluginInfo> {
    val allCompatibleUpdatesOfPlugin = pluginRepository.tryInvokeSeveralTimes(3, 5, TimeUnit.SECONDS, "fetch all compatible updates of plugin $pluginId with $ideVersion") {
      getAllCompatibleVersionsOfPlugin(ideVersion, pluginId)
    }
    return allCompatibleUpdatesOfPlugin.map { it as UpdateInfo }
  }

  /**
   * Parses a file containing a list of plugin IDs to check.
   * ```
   * plugin.one
   * $plugin.two
   * //comment
   * plugin.three$
   * ```
   *
   * If '$' is specified as a prefix or a suffix, only the last version
   * of the plugin will be checked. Otherwise, all versions of the plugin will be checked.
   *
   * Returns (ID-s of plugins to check all builds, ID-s of plugins to check last builds)
   */
  private fun parseAllAndLastPluginIdsToCheck(opts: CmdOpts): Pair<List<String>, List<String>> {
    val pluginsCheckAllBuilds = arrayListOf<String>()
    val pluginsCheckLastBuilds = arrayListOf<String>()

    pluginsCheckAllBuilds.addAll(opts.pluginToCheckAllBuilds)
    pluginsCheckLastBuilds.addAll(opts.pluginToCheckLastBuild)

    val pluginsFile = opts.pluginsToCheckFile
    if (pluginsFile != null) {
      try {
        File(pluginsFile).readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("//") }
            .forEach { fullLine ->
              val trimmed = fullLine.trim('$').trim()
              if (trimmed.isNotEmpty()) {
                if (fullLine.startsWith("$") || fullLine.endsWith("$")) {
                  pluginsCheckLastBuilds.add(trimmed)
                } else {
                  pluginsCheckAllBuilds.add(trimmed)
                }
              }
            }
      } catch (e: IOException) {
        throw RuntimeException("Failed to read plugins to check file " + pluginsFile + ": " + e.message, e)
      }
    }

    return pluginsCheckAllBuilds to pluginsCheckLastBuilds
  }

  /**
   * Requests the plugins' information for plugins with specified id-s.
   *
   * Parameter [ideVersion] is used to select the compatible versions of these plugins, that is,
   * only the updates whose [since; until] range contains the [ideVersion] will be selected.
   * [checkAllBuildsPluginIds] is a list of plugin id-s of plugins for which every build (aka plugin version) will be selected.
   * [checkLastBuildsPluginIds] is a list of plugin id-s of plugins for which only the newest build (version) will be selected.
   */
  private fun requestUpdatesToCheckByIds(checkAllBuildsPluginIds: List<String>,
                                         checkLastBuildsPluginIds: List<String>,
                                         ideVersion: IdeVersion): List<PluginInfo> {
    if (checkAllBuildsPluginIds.isEmpty() && checkLastBuildsPluginIds.isEmpty()) {
      return pluginRepository.getLastCompatiblePlugins(ideVersion)
    } else {
      val result = arrayListOf<PluginInfo>()

      checkAllBuildsPluginIds.flatMapTo(result) {
        pluginRepository.getAllCompatibleVersionsOfPlugin(ideVersion, it)
      }

      checkLastBuildsPluginIds.distinct().mapNotNullTo(result) {
        pluginRepository.getAllCompatibleVersionsOfPlugin(ideVersion, it)
            .sortedByDescending { (it as UpdateInfo).updateId }
            .firstOrNull()
      }

      return result
    }
  }

}