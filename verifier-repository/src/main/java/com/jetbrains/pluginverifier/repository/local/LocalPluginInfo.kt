package com.jetbrains.pluginverifier.repository.local

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.simpleName
import com.jetbrains.pluginverifier.repository.PluginInfo
import java.nio.file.Path

/**
 * Identifier of a local plugin,
 * which either has a [backed file] [pluginFile]
 * or is an in-memory plugin.
 */
class LocalPluginInfo(pluginId: String,
                      pluginName: String,
                      version: String,
                      pluginRepository: LocalPluginRepository,
                      sinceBuild: IdeVersion,
                      untilBuild: IdeVersion?,
                      vendor: String?,
                      val pluginFile: Path?,
                      val definedModules: Set<String>) : PluginInfo(pluginId, pluginName, version, pluginRepository, sinceBuild, untilBuild, vendor, pluginFile?.toUri()?.toURL()) {

  override val presentableName = "$pluginId $version" + if (pluginFile != null) " (${pluginFile.simpleName})" else ""

}