package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.plugin.PluginCoordinate

/**
 * @author Sergey Patrikeev
 */
interface ReporterSetProvider {
  fun provide(pluginCoordinate: PluginCoordinate, ideVersion: IdeVersion): ReporterSet
}