package org.jetbrains.plugins.verifier.service.service.sarif

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import org.jetbrains.plugins.verifier.service.service.verifier.VerificationResultTypeDto
import org.jetbrains.plugins.verifier.service.service.verifier.convert
import org.jetbrains.plugins.verifier.service.service.verifier.convertResultType

fun PluginVerificationResult.Verified.toSarif(): PluginVerificationResultSARIF {
  return generateReport(
    rules = buildRules(),
    invocations = emptyList() // TODO()
  )
}

fun PluginVerificationResult.InvalidPlugin.toSarif(): PluginVerificationResultSARIF {
  return generateReport(
    rules = buildPluginStructureRules(),
    invocations = buildPluginStructureInspections()
  )
}

fun PluginVerificationResult.NotFound.toSarif(): PluginVerificationResultSARIF {
  return generateReport(
    rules = buildSingleRule(),
    invocations = buildSingleInvocation(),
  )
}

fun PluginVerificationResult.FailedToDownload.toSarif(): PluginVerificationResultSARIF {
  return generateReport(
    rules = buildSingleRule(),
    invocations = buildSingleInvocation(),
  )
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

private fun PluginVerificationResult.buildSingleInvocation(): List<InspectionResult> {
  return listOf(
    InspectionResult(
      ruleId = this.javaClass.canonicalName,
      level = SeverityValue.ERROR,
      message = Message(this.verificationVerdict),
      location = emptyList()
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
    this.convertResultType() != VerificationResultTypeDto.PROBLEMS
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