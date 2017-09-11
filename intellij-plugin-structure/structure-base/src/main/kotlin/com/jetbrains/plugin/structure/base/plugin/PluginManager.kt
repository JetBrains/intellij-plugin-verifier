package com.jetbrains.plugin.structure.base.plugin

import java.io.File

/**
 * @author Sergey Patrikeev
 */
interface PluginManager<out PluginType : Plugin> {
  fun createPlugin(pluginFile: File): PluginCreationResult<PluginType>
}