package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.base.utils.pluralize
import com.jetbrains.plugin.structure.base.utils.pluralizeWithNumber
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning
import com.jetbrains.pluginverifier.warnings.PluginStructureError
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalApiUsage
import com.jetbrains.pluginverifier.usages.internal.InternalApiUsage
import com.jetbrains.pluginverifier.usages.nonExtendable.NonExtendableApiUsage
import com.jetbrains.pluginverifier.usages.overrideOnly.OverrideOnlyMethodUsage

sealed class PluginVerificationResult {

  abstract val plugin: PluginInfo

  abstract val verificationTarget: PluginVerificationTarget

  abstract val verificationVerdict: String

  final override fun toString() = verificationVerdict

  data class Verified(
      override val plugin: PluginInfo,
      override val verificationTarget: PluginVerificationTarget,
      val dependenciesGraph: DependenciesGraph,
      val compatibilityProblems: Set<CompatibilityProblem> = emptySet(),
      val ignoredProblems: Map<CompatibilityProblem, String>,
      val compatibilityWarnings: Set<CompatibilityWarning> = emptySet(),
      val deprecatedUsages: Set<DeprecatedApiUsage> = emptySet(),
      val experimentalApiUsages: Set<ExperimentalApiUsage> = emptySet(),
      val internalApiUsages: Set<InternalApiUsage> = emptySet(),
      val nonExtendableApiUsages: Set<NonExtendableApiUsage> = emptySet(),
      val overrideOnlyMethodUsages: Set<OverrideOnlyMethodUsage> = emptySet()
  ) : PluginVerificationResult() {

    val hasDirectMissingDependencies: Boolean
      get() = directMissingDependencies.isNotEmpty()

    val hasCompatibilityProblems: Boolean
      get() = !hasDirectMissingDependencies && compatibilityProblems.isNotEmpty()

    val hasCompatibilityWarnings: Boolean
      get() = !hasDirectMissingDependencies && !hasCompatibilityProblems && compatibilityWarnings.isNotEmpty()

    val isOk: Boolean
      get() = !hasDirectMissingDependencies && !hasCompatibilityProblems && !hasCompatibilityWarnings

    val directMissingDependencies: List<MissingDependency>
      get() = dependenciesGraph.verifiedPlugin.missingDependencies

    override val verificationVerdict
      get() = when {
        hasCompatibilityWarnings -> "Found " + "warning".pluralizeWithNumber(compatibilityWarnings.size)
        hasCompatibilityProblems -> buildString {
          append("Found ").append(compatibilityProblems.size).append(" compatibility ").append("problem".pluralize(compatibilityProblems.size))
          if (compatibilityWarnings.isNotEmpty()) {
            append(" and ").append(compatibilityWarnings.size).append(" ").append("warning".pluralize(compatibilityWarnings.size))
          }
        }
        hasDirectMissingDependencies -> buildString {
          val (modules, plugins) = directMissingDependencies.partition { it.dependency.isModule }
          append("Missing ")
          if (modules.isNotEmpty()) {
            val (optional, nonOptional) = modules.partition { it.dependency.isOptional }
            if (nonOptional.isNotEmpty()) {
              append(nonOptional.size)
              append(" mandatory " + "module".pluralize(nonOptional.size))
            }
            if (optional.isNotEmpty()) {
              if (nonOptional.isNotEmpty()) {
                append(" and ")
              }
              append(optional.size)
              append(" optional " + "module".pluralize(optional.size))
            }
          }
          if (plugins.isNotEmpty()) {
            if (modules.isNotEmpty()) {
              append(" and ")
            }
            val (optional, nonOptional) = plugins.partition { it.dependency.isOptional }
            if (nonOptional.isNotEmpty()) {
              append(nonOptional.size)
              append(" mandatory " + "plugin".pluralize(nonOptional.size))
            }
            if (optional.isNotEmpty()) {
              if (nonOptional.isNotEmpty()) {
                append(" and ")
              }
              append(optional.size)
              append(" optional " + "plugin".pluralize(optional.size))
            }
          }
          append(" ")
          append("dependency".pluralize(modules.size + plugins.size))
          if (compatibilityProblems.isNotEmpty()) {
            append(" and found ${compatibilityProblems.size} compatibility " + "problem".pluralize(compatibilityProblems.size))
          }
        }
        else -> "OK"
      }
  }

  data class InvalidPlugin(
      override val plugin: PluginInfo,
      override val verificationTarget: PluginVerificationTarget,
      val pluginStructureErrors: Set<PluginStructureError>
  ) : PluginVerificationResult() {
    override val verificationVerdict
      get() = "Plugin is invalid"
  }

  data class NotFound(
      override val plugin: PluginInfo,
      override val verificationTarget: PluginVerificationTarget,
      val notFoundReason: String
  ) : PluginVerificationResult() {
    override val verificationVerdict
      get() = "Plugin is not found: $notFoundReason"
  }

  data class FailedToDownload(
      override val plugin: PluginInfo,
      override val verificationTarget: PluginVerificationTarget,
      val failedToDownloadReason: String,
      val failedToDownloadError: Throwable
  ) : PluginVerificationResult() {
    override val verificationVerdict
      get() = "Failed to download plugin: $failedToDownloadReason"
  }

}