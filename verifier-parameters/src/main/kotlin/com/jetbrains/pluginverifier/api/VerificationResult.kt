package com.jetbrains.pluginverifier.api

import com.intellij.structure.problems.PluginProblem

sealed class VerificationResult {

  abstract val pluginDescriptor: PluginDescriptor

  abstract val ideDescriptor: IdeDescriptor

  data class Verified(override val pluginDescriptor: PluginDescriptor,
                      override val ideDescriptor: IdeDescriptor,
                      val verdict: Verdict,
                      val pluginInfo: PluginInfo) : VerificationResult()

  data class BadPlugin(override val pluginDescriptor: PluginDescriptor,
                       override val ideDescriptor: IdeDescriptor,
                       val problems: List<PluginProblem>) : VerificationResult()

  data class NotFound(override val pluginDescriptor: PluginDescriptor,
                      override val ideDescriptor: IdeDescriptor,
                      val reason: String) : VerificationResult()

}