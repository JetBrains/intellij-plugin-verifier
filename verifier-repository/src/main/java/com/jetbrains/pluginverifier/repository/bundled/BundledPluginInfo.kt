package com.jetbrains.pluginverifier.repository.bundled

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.repository.PluginInfo

/**
 * Identifier of a plugin bundled to the [IDE] [ide].
 * The plugin can be accessed by [idePlugin].
 */
class BundledPluginInfo(
    bundledPluginsRepository: BundledPluginsRepository,
    val ide: Ide,
    val idePlugin: IdePlugin
) : PluginInfo(
    idePlugin.pluginId!!,
    idePlugin.pluginName!!,
    idePlugin.pluginVersion ?: "<unspecified>",
    bundledPluginsRepository,
    idePlugin.sinceBuild,
    idePlugin.untilBuild,
    idePlugin.vendor
)