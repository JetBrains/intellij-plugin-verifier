package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.verification.VerificationReporterSet
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportersProvider

object EmptyReporterSetProvider : VerificationReportersProvider {
  override val globalMessageReporters: List<Reporter<String>> = emptyList()

  override val globalProgressReporters: List<Reporter<Double>> = emptyList()

  override fun close() = Unit

  override fun getReporterSetForPluginVerification(pluginCoordinate: PluginCoordinate, ideVersion: IdeVersion): VerificationReporterSet =
      VerificationReporterSet(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
}