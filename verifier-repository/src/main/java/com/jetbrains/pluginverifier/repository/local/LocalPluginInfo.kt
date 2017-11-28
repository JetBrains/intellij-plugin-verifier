package com.jetbrains.pluginverifier.repository.local

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import java.nio.file.Path

/**
 * Identifier of a locally stored plugin.
 */
data class LocalPluginInfo(override val pluginId: String,
                           override val version: String,
                           val pluginName: String,
                           val sinceBuild: IdeVersion,
                           val untilBuild: IdeVersion?,
                           val vendor: String?,
                           val pluginFile: Path,
                           val definedModules: Set<String>) : PluginInfo