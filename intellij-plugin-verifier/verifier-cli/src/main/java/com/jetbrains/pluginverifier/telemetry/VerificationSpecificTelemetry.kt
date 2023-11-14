package com.jetbrains.pluginverifier.telemetry

import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import com.jetbrains.plugin.structure.ide.PluginIdAndVersion
import com.jetbrains.plugin.structure.intellij.version.IdeVersion

data class VerificationSpecificTelemetry(val ideVersion: IdeVersion, val pluginIdAndVersion: PluginIdAndVersion, val telemetry: PluginTelemetry)

