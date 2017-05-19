package com.jetbrains.pluginverifier.utils

import com.jetbrains.pluginverifier.api.*
import java.io.File

/**
 * @author Sergey Patrikeev
 */
class VerificationResultToApiResultConverter {

  fun convert(results: List<VerificationResult>): List<Result> = results.map {
    Result(getPluginInfoByVerification(it), it.ideDescriptor.ideVersion, getVerdictByVerification(it))
  }

  private fun getPluginInfoByVerification(verificationResult: VerificationResult): PluginInfo = when (verificationResult) {
    is VerificationResult.Verified -> verificationResult.pluginInfo
    is VerificationResult.BadPlugin -> getPluginInfoByDescriptor(verificationResult.pluginDescriptor)
    is VerificationResult.NotFound -> getPluginInfoByDescriptor(verificationResult.pluginDescriptor)
  }

  private fun getVerdictByVerification(verificationResult: VerificationResult): Verdict = when (verificationResult) {
    is VerificationResult.Verified -> verificationResult.verdict
    is VerificationResult.BadPlugin -> Verdict.Bad(verificationResult.problems)
    is VerificationResult.NotFound -> Verdict.NotFound(verificationResult.reason)
  }

  private fun getPluginInfoByDescriptor(pluginDescriptor: PluginDescriptor): PluginInfo {
    return when (pluginDescriptor) {
      is PluginDescriptor.ByUpdateInfo -> PluginInfo(pluginDescriptor.updateInfo.pluginId, pluginDescriptor.updateInfo.version, pluginDescriptor.updateInfo)
      is PluginDescriptor.ByFileLock -> {
        val (pluginId, version) = guessPluginIdAndVersion(pluginDescriptor.fileLock.getFile())
        PluginInfo(pluginId, version, null)
      }
      is PluginDescriptor.ByInstance -> {
        PluginInfo(pluginDescriptor.createOk.success.plugin.pluginId, pluginDescriptor.createOk.success.plugin.pluginVersion, null)
      }
    }
  }

  private fun guessPluginIdAndVersion(file: File): Pair<String, String> {
    val name = file.nameWithoutExtension
    val version = name.substringAfterLast('-')
    return name.substringBeforeLast('-') to version
  }
}