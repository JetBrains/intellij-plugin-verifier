package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.reporting.Reporter
import java.io.Closeable

/**
 * @author Sergey Patrikeev
 */
interface VerificationReportersProvider : Closeable {

  val globalMessageReporters: List<Reporter<String>>

  val globalProgressReporters: List<Reporter<Double>>

  fun getReporterSetForPluginVerification(pluginCoordinate: PluginCoordinate, ideVersion: IdeVersion): VerificationReporterSet
}