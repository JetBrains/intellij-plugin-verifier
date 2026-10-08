package com.jetbrains.pluginverifier.reporting

import com.jetbrains.pluginverifier.PluginVerificationResult
import java.nio.file.Path

/**
 * Maps plugin verification results to target directory in the filesystem that contains the report files.
 *
 * When used by [DirectoryBasedPluginVerificationReportage], calls to [handleVerificationResult] are serialized,
 * even though the per-plugin report files are written concurrently by multiple verification workers.
 * Therefore, implementations are not required to be thread-safe.
 */
fun interface PluginVerificationReportageAggregator {
  fun handleVerificationResult(result: PluginVerificationResult, targetDirectory: Path)
}

