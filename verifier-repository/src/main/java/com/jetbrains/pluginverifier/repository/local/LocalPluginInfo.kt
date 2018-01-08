package com.jetbrains.pluginverifier.repository.local

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import java.nio.file.Path

/**
 * Identifier of a locally stored plugin.
 * The plugin can be accessed by [pluginFile].
 */
class LocalPluginInfo(pluginId: String,
                      pluginName: String,
                      version: String,
                      pluginRepository: LocalPluginRepository,
                      sinceBuild: IdeVersion,
                      untilBuild: IdeVersion?,
                      vendor: String?,
                      val pluginFile: Path,
                      val definedModules: Set<String>) : PluginInfo(pluginId, pluginName, version, pluginRepository, sinceBuild, untilBuild, vendor) {

  override val presentableName = "Plugin at $pluginFile"

}