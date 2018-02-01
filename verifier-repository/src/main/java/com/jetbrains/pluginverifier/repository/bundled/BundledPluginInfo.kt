package com.jetbrains.pluginverifier.repository.bundled

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.repository.PluginInfo

/**
 * Identifier of a plugin bundled to the [IDE] [ide].
 * The plugin can be accessed by [idePlugin].
 * It may have a backed file, or it may be an in-memory plugin.
 */
class BundledPluginInfo(
    bundledPluginsRepository: BundledPluginsRepository,
    idePlugin: IdePlugin,
    val ide: Ide
) : PluginInfo(
    idePlugin.pluginId!!,
    idePlugin.pluginName!!,
    idePlugin.pluginVersion ?: ide.version.asString(),
    bundledPluginsRepository,
    idePlugin.sinceBuild,
    idePlugin.untilBuild,
    idePlugin.vendor,
    idePlugin.originalFile?.toURI()?.toURL(),
    idePlugin
)