package com.jetbrains.pluginverifier.repository.local

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.misc.simpleName
import com.jetbrains.pluginverifier.repository.PluginInfo
import java.nio.file.Path

/**
 * Identifier of a local plugin,
 * which either has a [backed file] [pluginFile]
 * or is an in-memory plugin.
 */
class LocalPluginInfo(idePlugin: IdePlugin, pluginRepository: LocalPluginRepository)
  : PluginInfo(
    idePlugin.pluginId!!,
    idePlugin.pluginName!!,
    idePlugin.pluginVersion!!,
    pluginRepository,
    idePlugin.sinceBuild!!,
    idePlugin.untilBuild,
    idePlugin.vendor,
    idePlugin.originalFile?.toURI()?.toURL(),
    idePlugin
) {

  val definedModules: Set<String> = idePlugin.definedModules

  val pluginFile: Path? = idePlugin.originalFile?.toPath()

  override val presentableName = "${idePlugin.pluginId} ${idePlugin.pluginVersion}" + if (pluginFile != null) " (${pluginFile.simpleName})" else ""

}