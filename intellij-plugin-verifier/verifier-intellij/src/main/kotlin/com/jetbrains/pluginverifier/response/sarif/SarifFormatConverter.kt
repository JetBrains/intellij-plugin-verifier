package com.jetbrains.pluginverifier.response.sarif

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.response.VerificationResultTypeDto
import com.jetbrains.pluginverifier.response.convert
import com.jetbrains.pluginverifier.response.convertResultType

fun PluginVerificationResult.toSarif(): PluginVerificationResultSARIF {
  return when (this) {
    is PluginVerificationResult.Verified -> generateReport(
      rules = buildRules(),
      invocations = buildVerifiedInspections(),
    )
    is PluginVerificationResult.InvalidPlugin -> generateReport(
      rules = buildPluginStructureRules(),
      invocations = buildPluginStructureInspections(),
    )
    else -> generateReport(
      rules = buildSingleRule(),
      invocations = buildSingleInvocation(),
    )
  }
}


private fun PluginVerificationResult.generateReport(
  rules: List<Rule>,
  invocations: List<InspectionResult>
): PluginVerificationResultSARIF {
  return PluginVerificationResultSARIF(
    runs = listOf(
      Runner(
        properties = toPluginVerifierPropertiesBag(),
        automationDetails = toAutomationDetails(),
        versionControlProvenance = toVersionControlProvenance(),
        invocations = toInvocationStatus(),
        tool = toToolWithRules(rules),
        results = invocations
      )
    )
  )
}

private fun PluginVerificationResult.toInvocationStatus(): List<InvocationStatus> {
  val code = when (this) {
    is PluginVerificationResult.NotFound, is PluginVerificationResult.FailedToDownload -> 127
    is PluginVerificationResult.InvalidPlugin -> 1
    is PluginVerificationResult.Verified -> 0
  }
  val executionSuccessful = if (this is PluginVerificationResult.Verified) {
    val resultType = this.convertResultType()
    resultType != VerificationResultTypeDto.PROBLEMS && resultType != VerificationResultTypeDto.CRITICAL
  } else false
  return listOf(
    InvocationStatus(
      exitCode = code,
      executionSuccessful = executionSuccessful,
      exitCodeDescription = this.verificationVerdict
    )
  )
}

private fun PluginVerificationResult.toAutomationDetails(): AutomationDetails {
  val plugin = this.plugin
  return AutomationDetails(id = plugin.presentableName + " " + plugin.presentableSinceUntilRange)
}

private fun PluginVerificationResult.toPluginVerifierPropertiesBag(): PluginVerifierPropertiesBag {
  val dependenciesGraph = if (this is PluginVerificationResult.Verified) dependenciesGraph.convert() else null
  val dynamicPluginStatus = if (this is PluginVerificationResult.Verified) dynamicPluginStatus?.convert() else null
  val ideVersion = verificationTarget as PluginVerificationTarget.IDE
  val type = when (this) {
    is PluginVerificationResult.Verified -> this.convertResultType()
    is PluginVerificationResult.NotFound, is PluginVerificationResult.FailedToDownload -> VerificationResultTypeDto.NON_DOWNLOADABLE
    is PluginVerificationResult.InvalidPlugin -> VerificationResultTypeDto.INVALID_PLUGIN
  }
  return PluginVerifierPropertiesBag(
    ideVersion = ideVersion.ideVersion.asString(),
    javaVersion = ideVersion.jdkVersion.javaVersion,
    resultType = type,
    verdict = this.verificationVerdict,
    dependenciesGraph = dependenciesGraph,
    dynamicPluginStatus = dynamicPluginStatus,
  )
}

private fun toVersionControlProvenance(): List<VersionControlProvenance> {
  return listOf(
    VersionControlProvenance(
      repositoryUri = "TODO()" // TODO()
    )
  )
}

private fun toToolWithRules(rules: List<Rule>): Tool {
  return Tool(Driver(rules = rules))
}