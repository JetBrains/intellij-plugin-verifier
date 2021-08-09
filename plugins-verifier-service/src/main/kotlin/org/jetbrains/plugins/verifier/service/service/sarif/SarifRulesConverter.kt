package org.jetbrains.plugins.verifier.service.service.sarif

import com.jetbrains.pluginverifier.PluginVerificationResult

internal fun PluginVerificationResult.Verified.buildRules(): List<Rule> {
  val warningsStructureRules = buildPluginStructureWarningsRules()
  val compatibilityWarningsRules = buildCompatibilityWarningsRules()
  val compatibilityProblemsRules = buildCompatibilityProblemRules()
  val deprecatedApiRules = buildDeprecatedApiRules()
  return warningsStructureRules + compatibilityWarningsRules + compatibilityProblemsRules + deprecatedApiRules
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

private fun PluginVerificationResult.Verified.buildDeprecatedApiRules(): List<Rule> {
  if (deprecatedUsages.isEmpty()) return emptyList()
  return deprecatedUsages.map {
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
