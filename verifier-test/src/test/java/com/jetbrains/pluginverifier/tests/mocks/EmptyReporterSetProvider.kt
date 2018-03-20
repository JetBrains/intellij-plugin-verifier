package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.ignoring.PluginIgnoredEvent
import com.jetbrains.pluginverifier.reporting.verification.VerificationReporterSet
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportersProvider
import com.jetbrains.pluginverifier.repository.PluginInfo

object EmptyReporterSetProvider : VerificationReportersProvider {
  override val globalMessageReporters = emptyList<Reporter<String>>()

  override val globalProgressReporters = emptyList<Reporter<Double>>()

  override val ignoredPluginsReporters = emptyList<Reporter<PluginIgnoredEvent>>()

  override fun close() = Unit

  override fun getReporterSetForPluginVerification(pluginInfo: PluginInfo, ideVersion: IdeVersion): VerificationReporterSet =
      VerificationReporterSet(
          emptyList(),
          emptyList(),
          emptyList(),
          emptyList(),
          emptyList(),
          emptyList(),
          emptyList(),
          emptyList(),
          emptyList(),
          emptyList()
      )
}