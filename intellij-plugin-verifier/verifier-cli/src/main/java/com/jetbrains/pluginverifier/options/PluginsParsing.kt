/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.options

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.readLines
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.problems.*
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.resolution.LastVersionSelector
import com.jetbrains.pluginverifier.dependencies.resolution.PluginVersionSelector
import com.jetbrains.pluginverifier.misc.retry
import com.jetbrains.pluginverifier.output.READING_PLUGIN_FROM
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginInfo
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import com.jetbrains.pluginverifier.tasks.InvalidPluginFile
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths


private val LOG: Logger = LoggerFactory.getLogger(PluginsParsing::class.java)

/**
 * Utility class used to fill [pluginsSet] with a list of plugins to check.
 */
class PluginsParsing(
  private val pluginRepository: PluginRepository,
  private val reportage: PluginVerificationReportage,
  private val pluginsSet: PluginsSet,
  private val configuration: PluginParsingConfiguration = PluginParsingConfiguration()
) {

  /**
   * Parses command line options and add specified plugins compatible with [ideVersion].
   */
  fun addPluginsFromCmdOpts(opts: CmdOpts, ideVersion: IdeVersion) {
    for (pluginId in opts.pluginToCheckAllBuilds) {
      addAllCompatibleVersionsOfPlugin(pluginId, ideVersion)
    }

    for (pluginId in opts.pluginToCheckLastBuild) {
      addLastCompatibleVersionOfPlugin(pluginId, ideVersion)
    }

    val pluginsToCheckFile = opts.pluginsToCheckFile?.let { Paths.get(it) }
    if (pluginsToCheckFile != null) {
      addPluginsListedInFile(pluginsToCheckFile, listOf(ideVersion))
    }
  }

  /**
   * Adds update #[updateId] to [pluginsSet].
   */
  fun addUpdateById(updateId: Int) {
    val updateInfo = pluginRepository.retry("get plugin info for #$updateId") {
      (pluginRepository as? MarketplaceRepository)?.getPluginInfoByUpdateId(updateId)
    } ?: throw IllegalArgumentException("Update #$updateId is not found in the $pluginRepository")
    pluginsSet.schedulePlugin(updateInfo)
  }

  /**
   * Adds last version of [pluginId] to [pluginsSet].
   */
  fun addLastPluginVersion(pluginId: String) {
    val selector = LastVersionSelector()
    val selectResult = retry("Latest version of $pluginId") {
      selector.selectPluginVersion(pluginId, pluginRepository)
    }
    if (selectResult is PluginVersionSelector.Result.Selected) {
      pluginsSet.schedulePlugin(selectResult.pluginInfo)
    }
  }

  /**
   * Parses lines of [pluginsListFile] and adds specified plugins to the [pluginsSet].
   */
  fun addPluginsListedInFile(pluginsListFile: Path, ideVersions: List<IdeVersion>) {
    reportage.logVerificationStage("Reading plugins list to check from file ${pluginsListFile.toAbsolutePath()} against IDE versions ${ideVersions.joinToString { it.asString() }}")
    val specs = pluginsListFile.readLines()
      .map { it.trim() }
      .filterNot { it.isEmpty() }
      .filterNot { it.startsWith("//") }

    for (spec in specs) {
      addPluginBySpec(spec, pluginsListFile, ideVersions)
    }
  }

  /**
   * Adds all plugins that correspond to one of the following specs:
   *
   * ```
   * - id:<plugin-id>                    // all compatible version of <plugin-id>
   * - version:<plugin-id>:<version>     // all compatible version of <plugin-id>
   * - $id or id$                        // only the last version of the plugin compatible with IDEs will be checked
   * - #<update-id>                      // update #<update-id>
   * - path:<plugin-path>                // plugin from <plugin-path>, where <plugin-path> may be relative to base path.
   * - <other>                           // treated as a path: or id:
   * ```
   */
  fun addPluginBySpec(spec: String, basePath: Path, ideVersions: List<IdeVersion>) {
    if (spec.startsWith('$') || spec.endsWith('$')) {
      val pluginId = spec.trim('$').trim()
      ideVersions.forEach { addLastCompatibleVersionOfPlugin(pluginId, it) }
      return
    }

    if (spec.startsWith("#")) {
      val updateId = spec.substringAfter("#").toIntOrNull() ?: return
      addUpdateById(updateId)
      return
    }

    if (spec.startsWith("id:")) {
      val pluginId = spec.substringAfter("id:")
      ideVersions.forEach { addAllCompatibleVersionsOfPlugin(pluginId, it) }
      return
    }

    if (spec.startsWith("version:")) {
      val idAndVersion = spec.substringAfter("version:")
      val id = idAndVersion.substringBefore(":").trim()
      val version = idAndVersion.substringAfter(":").trim()
      require(version.isNotEmpty()) { "Empty version specified for a plugin to be checked: {$spec}" }
      addPluginVersion(id, version)
      return
    }

    val pluginFile = if (spec.startsWith("path:")) {
      val linePath = spec.substringAfter("path:")
      tryFindPluginByPath(basePath, linePath) ?: throw IllegalArgumentException("Invalid path: $linePath")
    } else {
      tryFindPluginByPath(basePath, spec)
    }

    if (pluginFile != null) {
      addPluginFile(pluginFile, true)
    } else {
      ideVersions.forEach { addAllCompatibleVersionsOfPlugin(spec, it) }
    }
  }

  private fun tryFindPluginByPath(baseFilePath: Path, linePath: String): Path? {
    val path = try {
      Paths.get(linePath)
    } catch (e: Exception) {
      return null
    }

    if (path.exists()) {
      return path
    }

    val siblingPath = baseFilePath.resolveSibling(linePath)
    if (siblingPath.exists()) {
      return siblingPath
    }
    return null
  }

  /**
   * Adds all versions of the plugin with ID `pluginId` compatible with `ideVersion`.
   */
  private fun addAllCompatibleVersionsOfPlugin(pluginId: String, ideVersion: IdeVersion) {
    val stepName = "All versions of plugin '$pluginId' compatible with $ideVersion"
    val compatibleVersions = pluginRepository.retry(stepName) {
      getAllVersionsOfPlugin(pluginId).filter { it.isCompatibleWith(ideVersion) }
    }
    reportage.logVerificationStage("$stepName: " + if (compatibleVersions.isEmpty()) "no compatible versions" else compatibleVersions.joinToString { it.presentableName })
    pluginsSet.schedulePlugins(compatibleVersions)
  }

  /**
   * Adds version [version] of plugin with XML ID [pluginId].
   */
  private fun addPluginVersion(pluginId: String, version: String) {
    val stepName = "Plugin '$pluginId' of version '$version'"
    val allVersionsOfPlugin = pluginRepository.retry(stepName) {
      getAllVersionsOfPlugin(pluginId)
    }
    val pluginInfo = allVersionsOfPlugin.find { it.version == version }
    reportage.logVerificationStage(
      stepName + ": " + (pluginInfo?.presentableName ?: "no version '$version' of plugin '$pluginId' available")
    )
    pluginInfo ?: return
    pluginsSet.schedulePlugin(pluginInfo)
  }

  /**
   * Adds the last version of plugin with ID `pluginId` compatible with `ideVersion`.
   */
  private fun addLastCompatibleVersionOfPlugin(pluginId: String, ideVersion: IdeVersion) {
    val stepName = "Last version of plugin '$pluginId' compatible with $ideVersion"
    val lastVersion = pluginRepository.retry(stepName) {
      getLastCompatibleVersionOfPlugin(ideVersion, pluginId)
    }
    reportage.logVerificationStage("$stepName: ${lastVersion?.presentableName ?: "no compatible version"}")
    lastVersion ?: return
    pluginsSet.schedulePlugin(lastVersion)
  }

  /**
   * Adds plugin from [pluginFile].
   */
  fun addPluginFile(pluginFile: Path, validateDescriptor: Boolean) {
    if (!pluginFile.exists()) {
      throw RuntimeException("Plugin file '$pluginFile' with absolute path '${pluginFile.toAbsolutePath()}' doesn't exist")
    }

    reportage.logVerificationStage(READING_PLUGIN_FROM.format(pluginFile))
    val pluginCreationResult = IdePluginManager
      .createManager()
      .createPlugin(pluginFile, validateDescriptor, problemResolver = configuration.problemResolver)
    with(pluginCreationResult) {
      when (this) {
        is PluginCreationSuccess -> {
          pluginsSet.scheduleLocalPlugin(plugin).also {
            reportLocalPluginTelemetry(plugin, telemetry)
          }
        }
        is PluginCreationFail -> {
          reportage.logVerificationStage("Plugin is invalid in $pluginFile: ${errorsAndWarnings.joinToString()}")
          pluginsSet.invalidPluginFiles.add(InvalidPluginFile(pluginFile, errorsAndWarnings))
        }
      }
    }
  }

  private fun reportLocalPluginTelemetry(plugin: IdePlugin, telemetry: PluginTelemetry) {
    reportage.reportTelemetry(LocalPluginInfo(plugin), telemetry)
  }

  private val PluginParsingConfiguration.problemResolver: PluginCreationResultResolver
    get() {
      val defaultResolver = IntelliJPluginCreationResultResolver()
      return if (pluginSubmissionType == SubmissionType.EXISTING) {
        val problemLevelRemapping = try {
          val pluginProblemsLoader = PluginProblemsLoader.fromClassPath()
          val problemLevelRemappingDefinitions = pluginProblemsLoader.load()
          val pluginProblemSet = problemLevelRemappingDefinitions["existing-plugin"]
            ?: emptyProblemLevelRemapping("existing-plugin")
          val parser = RemappedPluginProblemLevelParser()
          val remapping = parser.parse(pluginProblemSet)
          remapping
        } catch (e: IOException) {
          LOG.error(e.message, e)
          emptyMap()
        }

        LevelRemappingPluginCreationResultResolver(defaultResolver, additionalLevelRemapping = problemLevelRemapping)
      } else {
        defaultResolver
      }
    }

}