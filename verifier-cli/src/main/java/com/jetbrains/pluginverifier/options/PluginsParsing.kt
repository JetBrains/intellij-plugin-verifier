package com.jetbrains.pluginverifier.options

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.resolution.LastVersionSelector
import com.jetbrains.pluginverifier.dependencies.resolution.PluginVersionSelector
import com.jetbrains.pluginverifier.misc.exists
import com.jetbrains.pluginverifier.misc.readLines
import com.jetbrains.pluginverifier.misc.tryInvokeSeveralTimes
import com.jetbrains.pluginverifier.reporting.verification.Reportage
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

/**
 * Utility class used to fill [pluginsSet] with a list of plugins to check.
 */
class PluginsParsing(
    private val pluginRepository: PluginRepository,
    private val reportage: Reportage,
    private val pluginsSet: PluginsSet
) {

  /**
   * Adds update #[updateId] to [pluginsSet].
   */
  fun addUpdate(updateId: Int) {
    val updateInfo = getPluginInfoByUpdateId(updateId)
        ?: throw IllegalArgumentException("Update #$updateId is not found in the Plugin Repository")
    pluginsSet.schedulePlugin(updateInfo)
  }

  /**
   * Adds last version of [pluginId] to [pluginsSet].
   */
  fun addLastPluginVersion(pluginId: String) {
    val selector = LastVersionSelector()
    val selectResult = selector.selectPluginVersion(pluginId, pluginRepository)
    if (selectResult is PluginVersionSelector.Result.Selected) {
      pluginsSet.schedulePlugin(selectResult.pluginInfo)
    }
  }

  /**
   * Parses the command line options [opts] for a set of plugin IDs of
   * to be checked against [ideVersion]
   * and requests corresponding [UpdateInfo]s.
   */
  fun addByPluginIds(opts: CmdOpts, ideVersion: IdeVersion): PluginsSet {
    val (allVersions, lastVersions) = parseAllAndLastPluginIdsToCheck(opts)

    val pluginInfos = tryInvokeSeveralTimes(3, 5, TimeUnit.SECONDS, "fetch updates to check against $ideVersion") {
      requestUpdatesToCheckByIds(allVersions, lastVersions, ideVersion)
    }
    pluginsSet.schedulePlugins(pluginInfos)
    return pluginsSet
  }

  /**
   * Parses lines of [pluginsListFile] and adds
   * specified plugins to the [pluginsSet].
   *
   * - `id:<plugin-id>` - all version of <plugin-id> compatible with all [ideVersions] are added
   * - `#<update-id>` - update #<update-id> is added
   * - <plugin-path> - plugin from local <plugin-path> is added
   */
  fun addPluginsFromFile(
      pluginsListFile: Path,
      ideVersions: List<IdeVersion>
  ) {
    val lines = pluginsListFile.readLines().map { it.trim() }.filterNot { it.isEmpty() }
    for (ideVersion in ideVersions) {
      for (line in lines) {
        if (line.startsWith("id:")) {
          val compatiblePluginVersions = getCompatiblePluginVersions(line.substringAfter("id:"), ideVersion)
          pluginsSet.schedulePlugins(compatiblePluginVersions)
          continue
        }

        if (line.startsWith("#")) {
          val updateId = line.substringAfter("#").toIntOrNull() ?: continue
          addUpdate(updateId)
          continue
        }

        var pluginFile = Paths.get(line)
        if (!pluginFile.isAbsolute) {
          pluginFile = pluginsListFile.resolveSibling(line)
        }
        addPluginFile(pluginFile, true)
      }
    }
  }

  /**
   * Adds plugin from local path [pluginFile] to [pluginsSet].
   */
  fun addPluginFile(pluginFile: Path, validateDescriptor: Boolean) {
    if (!pluginFile.exists()) {
      throw RuntimeException("Plugin file '$pluginFile' with absolute path '${pluginFile.toAbsolutePath()}' doesn't exist")
    }

    reportage.logVerificationStage("Reading plugin to check from $pluginFile")
    with(IdePluginManager.createManager().createPlugin(pluginFile.toFile(), validateDescriptor)) {
      when (this) {
        is PluginCreationSuccess -> pluginsSet.scheduleLocalPlugin(plugin)
        is PluginCreationFail -> {
          reportage.logVerificationStage("Plugin is invalid in $pluginFile: ${errorsAndWarnings.joinToString()}")
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
   * Returns (ID-s of plugins to check all builds, ID-s of plugins to check last builds)
   */
  private fun parseAllAndLastPluginIdsToCheck(opts: CmdOpts): Pair<List<String>, List<String>> {
    val pluginsCheckAllBuilds = arrayListOf<String>()
    val pluginsCheckLastBuilds = arrayListOf<String>()

    pluginsCheckAllBuilds.addAll(opts.pluginToCheckAllBuilds)
    pluginsCheckLastBuilds.addAll(opts.pluginToCheckLastBuild)

    val pluginsFile = opts.pluginsToCheckFile?.let { File(it) }
    if (pluginsFile != null) {
      parseAllAndLastBuildsFile(pluginsFile, pluginsCheckAllBuilds, pluginsCheckLastBuilds)
    }

    return pluginsCheckAllBuilds to pluginsCheckLastBuilds
  }

  /**
   * Parses [pluginsListFile] containing a list of plugin IDs to check.
   * ```
   * plugin.one
   * $plugin.two
   * //comment
   * plugin.three$
   * ```
   *
   * If '$' is specified as a prefix or a suffix, only the last version
   * of the plugin will be checked. Otherwise, all versions of the plugin will be checked.
   */
  private fun parseAllAndLastBuildsFile(
      pluginsListFile: File,
      allBuilds: MutableList<String>,
      lastBuilds: MutableList<String>
  ) {
    try {
      pluginsListFile.readLines()
          .map { it.trim() }
          .filter { it.isNotEmpty() && !it.startsWith("//") }
          .forEach { fullLine ->
            val trimmed = fullLine.trim('$').trim()
            if (trimmed.isNotEmpty()) {
              if (fullLine.startsWith("$") || fullLine.endsWith("$")) {
                lastBuilds.add(trimmed)
              } else {
                allBuilds.add(trimmed)
              }
            }
          }
    } catch (e: IOException) {
      throw RuntimeException("Failed to read plugins to check file " + pluginsListFile + ": " + e.message, e)
    }
  }

  /**
   * Requests the plugins' information for plugins with specified id-s.
   *
   * Parameter [ideVersion] is used to select the compatible versions of these plugins, that is,
   * only the updates whose [since; until] range contains the [ideVersion] will be selected.
   * [checkAllBuildsPluginIds] is a list of plugin id-s of plugins for which every build (aka plugin version) will be selected.
   * [checkLastBuildsPluginIds] is a list of plugin id-s of plugins for which only the newest build (version) will be selected.
   */
  private fun requestUpdatesToCheckByIds(
      checkAllBuildsPluginIds: List<String>,
      checkLastBuildsPluginIds: List<String>,
      ideVersion: IdeVersion
  ): List<PluginInfo> {
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