package com.jetbrains.pluginverifier.reporting

import com.jetbrains.pluginverifier.PluginVerificationResult
import java.nio.file.Path

/**
 * Maps plugin verification results to target directory in the filesystem that contains the report files.
 */
fun interface PluginVerificationReportageAggregator {
  fun handleVerificationResult(result: PluginVerificationResult, targetDirectory: Path)
}

