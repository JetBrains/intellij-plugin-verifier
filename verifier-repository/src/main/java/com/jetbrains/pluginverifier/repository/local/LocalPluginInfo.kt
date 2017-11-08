package com.jetbrains.pluginverifier.repository.local

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import java.io.File

data class LocalPluginInfo(override val pluginId: String,
                           override val version: String,
                           val pluginName: String,
                           val sinceBuild: IdeVersion,
                           val untilBuild: IdeVersion?,
                           val vendor: String?,
                           val pluginFile: File,
                           val definedModules: Set<String>) : PluginInfo