package org.jetbrains.plugins.verifier.service.service.sarif

import com.jetbrains.pluginverifier.PluginVerificationResult


internal fun PluginVerificationResult.Verified.buildVerifiedInspections(): List<InspectionResult> {
  val pluginStructureWarningsInspection = buildPluginStructureWarningsInspection()
  val compatibilityWarningsInspection = buildCompatibilityWarningsInspection()
  val compatibilityProblemsInspection = buildCompatibilityProblemInspection()
  val apiUsagesInspections = buildApiUsageInspection()
  return pluginStructureWarningsInspection + compatibilityWarningsInspection + compatibilityProblemsInspection + apiUsagesInspections
}


internal fun PluginVerificationResult.InvalidPlugin.buildPluginStructureInspections(): List<InspectionResult> {
  return pluginStructureErrors.map {
    InspectionResult(
      ruleId = it.javaClass.simpleName,
      level = SeverityValue.ERROR,
      message = Message(it.message),
      location = emptyList()
    )
  }
}

internal fun PluginVerificationResult.buildSingleInvocation(): List<InspectionResult> {
  return listOf(
    InspectionResult(
      ruleId = this.javaClass.simpleName,
      level = SeverityValue.ERROR,
      message = Message(this.verificationVerdict),
      location = emptyList()
    )
  )
}


private fun PluginVerificationResult.Verified.buildApiUsageInspection(): List<InspectionResult> {
  val apiUsages = deprecatedUsages + experimentalApiUsages + internalApiUsages + nonExtendableApiUsages + overrideOnlyMethodUsages
  return apiUsages.map {
    InspectionResult(
      ruleId = it.javaClass.simpleName,
      level = SeverityValue.ERROR,
      message = Message(it.fullDescription),
      location = emptyList()
    )
  }
}

private fun PluginVerificationResult.Verified.buildCompatibilityProblemInspection(): List<InspectionResult> {
  return compatibilityProblems.map {
    InspectionResult(
      ruleId = it.javaClass.simpleName,
      level = SeverityValue.ERROR,
      message = Message(it.fullDescription),
      location = emptyList()
    )
  }
}

private fun PluginVerificationResult.Verified.buildCompatibilityWarningsInspection(): List<InspectionResult> {
  return compatibilityWarnings.map {
    InspectionResult(
      ruleId = it.javaClass.simpleName,
      level = SeverityValue.WARNING,
      message = Message(it.fullDescription),
      location = emptyList()
    )
  }
}

private fun PluginVerificationResult.Verified.buildPluginStructureWarningsInspection(): List<InspectionResult> {
  return pluginStructureWarnings.map {
    InspectionResult(
      ruleId = it.problemType,
      level = SeverityValue.WARNING,
      message = Message(it.message),
      location = emptyList()
    )
  }
}

