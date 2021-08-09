package org.jetbrains.plugins.verifier.service.service.sarif

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.usages.ApiUsage

internal fun PluginVerificationResult.Verified.buildRules(): List<Rule> {
  val warningsStructureRules = buildPluginStructureWarningsRules()
  val compatibilityWarningsRules = buildCompatibilityWarningsRules()
  val compatibilityProblemsRules = buildCompatibilityProblemRules()
  val deprecatedApiRules = buildApiUsageRules(deprecatedUsages)
  val experimentalApiRules = buildApiUsageRules(experimentalApiUsages)
  val internalApiUsagesRules = buildApiUsageRules(internalApiUsages)
  val nonExtendableApiUsagesRules = buildApiUsageRules(nonExtendableApiUsages)
  val overrideOnlyMethodUsagesRules = buildApiUsageRules(overrideOnlyMethodUsages)
  return warningsStructureRules + compatibilityWarningsRules + compatibilityProblemsRules +
    deprecatedApiRules + experimentalApiRules + internalApiUsagesRules +
    nonExtendableApiUsagesRules + overrideOnlyMethodUsagesRules
}

internal fun PluginVerificationResult.InvalidPlugin.buildPluginStructureRules(): List<Rule> {
  if (pluginStructureErrors.isEmpty()) return emptyList()
  val defaultError = pluginStructureErrors.first()
  return listOf(
    Rule(
      id = defaultError.problemType,
      shortDescription = Message(defaultError.description),
      fullDescription = Message(defaultError.description),
      defaultConfiguration = RuleConfiguration(
        level = SeverityValue.ERROR,
        parameters = RuleParameters(
          ideaSeverity = SeverityIdea.ERROR
        )
      )
    )
  )
}


private fun buildApiUsageRules(apiUsage: Set<ApiUsage>): List<Rule> {
  if (apiUsage.isEmpty()) return emptyList()
  return apiUsage.map {
    Rule(
      id = it.javaClass.canonicalName,
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
  if (compatibilityProblems.isEmpty()) return emptyList()
  return compatibilityProblems.map {
    Rule(
      id = it.problemType,
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

private fun PluginVerificationResult.Verified.buildCompatibilityWarningsRules(): List<Rule> {
  if (compatibilityWarnings.isEmpty()) return emptyList()
  return compatibilityWarnings.map {
    Rule(
      id = it.javaClass.canonicalName,
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
