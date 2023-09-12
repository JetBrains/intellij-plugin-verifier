package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.PluginInfo

class MockPluginVerificationReportage : PluginVerificationReportage {
  override fun logVerificationStage(stageMessage: String) = Unit

  override fun logPluginVerificationIgnored(pluginInfo: PluginInfo, verificationTarget: PluginVerificationTarget, reason: String) =
    Unit

  override fun reportVerificationResult(pluginVerificationResult: PluginVerificationResult) = Unit

  override fun close() = Unit
}