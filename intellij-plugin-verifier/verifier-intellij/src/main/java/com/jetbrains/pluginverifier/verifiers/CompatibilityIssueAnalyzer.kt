package com.jetbrains.pluginverifier.verifiers

/**
 * Analyze possible compatibility issues (problems or warnings) in the verification context for a particular usage.
 */
interface CompatibilityIssueAnalyzer<in U> {
  fun analyze(context: PluginVerificationContext, usage: U)
}

