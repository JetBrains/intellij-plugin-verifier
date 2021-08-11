package org.jetbrains.plugins.verifier.service.service.sarif

import com.jetbrains.pluginverifier.PluginVerificationResult

internal fun PluginVerificationResult.Verified.buildRules(): List<Rule> {
  val warningsStructureRules = buildPluginStructureWarningsRules()
  val compatibilityWarningsRules = buildCompatibilityWarningsRules()
  val compatibilityProblemsRules = buildCompatibilityProblemRules()
  val apiUsagesRules = buildApiUsageRules()
  return warningsStructureRules + compatibilityWarningsRules + compatibilityProblemsRules + apiUsagesRules
}

internal fun PluginVerificationResult.InvalidPlugin.buildPluginStructureRules(): List<Rule> {
  if (pluginStructureErrors.isEmpty()) return emptyList()
  val defaultError = pluginStructureErrors.first()
  return listOf(
    Rule(
      id = defaultError.javaClass.simpleName,
      shortDescription = Message(defaultError.problemType),
      fullDescription = Message(defaultError.problemType),
      defaultConfiguration = RuleConfiguration(
        level = SeverityValue.ERROR,
        parameters = RuleParameters(
          ideaSeverity = SeverityIdea.ERROR
        )
      )
    )
  )
}

internal fun PluginVerificationResult.buildSingleRule(): List<Rule> {
  return listOf(
    Rule(
      id = this.javaClass.simpleName,
      shortDescription = Message(this.verificationVerdict),
      fullDescription = Message(this.verificationVerdict),
      defaultConfiguration = RuleConfiguration(
        level = SeverityValue.ERROR,
        parameters = RuleParameters(
          ideaSeverity = SeverityIdea.ERROR
        )
      )
    )
  )
}

private fun PluginVerificationResult.Verified.buildApiUsageRules(): List<Rule> {
  val apiUsages = deprecatedUsages + experimentalApiUsages + internalApiUsages + nonExtendableApiUsages + overrideOnlyMethodUsages
  return apiUsages.map {
    Rule(
      id = it.javaClass.simpleName,
      shortDescription = Message(it.shortDescription),
      fullDescription = Message(it.fullDescription),
      defaultConfiguration = RuleConfiguration(
        level = SeverityValue.ERROR,
        parameters = RuleParameters(
          ideaSeverity = SeverityIdea.ERROR
        )
      )
    )
  }.distinctBy { it.id }
}

private fun PluginVerificationResult.Verified.buildCompatibilityProblemRules(): List<Rule> {
  return compatibilityProblems.map {
    Rule(
      id = it.javaClass.simpleName,
      shortDescription = Message(it.problemType),
      fullDescription = Message(it.problemType),
      defaultConfiguration = RuleConfiguration(
        level = SeverityValue.ERROR,
        parameters = RuleParameters(
          ideaSeverity = SeverityIdea.ERROR
        )
      )
    )
  }.distinctBy { it.id }
}

private fun PluginVerificationResult.Verified.buildCompatibilityWarningsRules(): List<Rule> {
  return compatibilityWarnings.map {
    Rule(
      id = it.javaClass.simpleName,
      shortDescription = Message(it.shortDescription),
      fullDescription = Message(it.fullDescription),
      defaultConfiguration = RuleConfiguration(
        level = SeverityValue.WARNING,
        parameters = RuleParameters(
          ideaSeverity = SeverityIdea.WARNING
        )
      )
    )
  }.distinctBy { it.id }
}

private fun PluginVerificationResult.Verified.buildPluginStructureWarningsRules(): List<Rule> {
  if (pluginStructureWarnings.isEmpty()) return emptyList()
  val defaultWarning = pluginStructureWarnings.first()
  return listOf(
    Rule(
      id = defaultWarning.problemType,
      shortDescription = Message(defaultWarning.description),
      fullDescription = Message(defaultWarning.description),
      defaultConfiguration = RuleConfiguration(
        level = SeverityValue.WARNING,
        parameters = RuleParameters(
          ideaSeverity = SeverityIdea.WARNING
        )
      )
    )
  )
}
