/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.base.utils.pluralize
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.dymamic.DynamicPluginStatus
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalApiUsage
import com.jetbrains.pluginverifier.usages.internal.InternalApiUsage
import com.jetbrains.pluginverifier.usages.nonExtendable.NonExtendableApiUsage
import com.jetbrains.pluginverifier.usages.overrideOnly.OverrideOnlyMethodUsage
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning
import com.jetbrains.pluginverifier.warnings.PluginStructureError
import com.jetbrains.pluginverifier.warnings.PluginStructureWarning

sealed class PluginVerificationResult(
  val plugin: PluginInfo,
  val verificationTarget: PluginVerificationTarget
) {

  abstract val verificationVerdict: String

  final override fun toString() = verificationVerdict

  class Verified(
    plugin: PluginInfo,
    verificationTarget: PluginVerificationTarget,
    val dependenciesGraph: DependenciesGraph,
    val compatibilityProblems: Set<CompatibilityProblem> = emptySet(),
    val ignoredProblems: Map<CompatibilityProblem, String> = emptyMap(),
    val compatibilityWarnings: Set<CompatibilityWarning> = emptySet(),
    val deprecatedUsages: Set<DeprecatedApiUsage> = emptySet(),
    val experimentalApiUsages: Set<ExperimentalApiUsage> = emptySet(),
    val internalApiUsages: Set<InternalApiUsage> = emptySet(),
    val ignoredInternalApiUsages: Map<InternalApiUsage, String> = emptyMap(),
    val nonExtendableApiUsages: Set<NonExtendableApiUsage> = emptySet(),
    val overrideOnlyMethodUsages: Set<OverrideOnlyMethodUsage> = emptySet(),
    val pluginStructureWarnings: Set<PluginStructureWarning> = emptySet(),
    //Applicable only for PluginVerificationTarget.IDE
    val dynamicPluginStatus: DynamicPluginStatus? = null
  ) : PluginVerificationResult(plugin, verificationTarget) {

    val hasDirectMissingMandatoryDependencies: Boolean
      get() = directMissingMandatoryDependencies.isNotEmpty()

    val hasCompatibilityProblems: Boolean
      get() = !hasDirectMissingMandatoryDependencies && compatibilityProblems.isNotEmpty()

    val hasCompatibilityWarnings: Boolean
      get() = !hasDirectMissingMandatoryDependencies && !hasCompatibilityProblems && compatibilityWarnings.isNotEmpty()

    val isOk: Boolean
      get() = !hasDirectMissingMandatoryDependencies && !hasCompatibilityProblems && !hasCompatibilityWarnings

    val directMissingMandatoryDependencies: List<MissingDependency>
      get() = dependenciesGraph.getDirectMissingDependencies().filterNot { it.dependency.isOptional }

    override val verificationVerdict
      get() = buildString {
        if (directMissingMandatoryDependencies.isEmpty() && compatibilityProblems.isEmpty() && compatibilityWarnings.isEmpty()) {
          append("Compatible")
        }
        if (directMissingMandatoryDependencies.isNotEmpty()) {
          append("${directMissingMandatoryDependencies.size} missing mandatory ")
          append("dependency".pluralize(directMissingMandatoryDependencies.size))
        }
        if (compatibilityProblems.isNotEmpty()) {
          if (isNotEmpty()) append(". ")
          append(compatibilityProblems.size)
          if (directMissingMandatoryDependencies.isNotEmpty()) {
            append(" possible")
          }
          append(" compatibility ").append("problem".pluralize(compatibilityProblems.size))

          val allDirectMissingDependencies = dependenciesGraph.getDirectMissingDependencies()
          if (allDirectMissingDependencies.isNotEmpty()) {
            append(", some of which may be caused by absence of ")
            if (allDirectMissingDependencies.all { it.dependency.isOptional }) {
              append("optional ")
            }
            append("dependency".pluralize(allDirectMissingDependencies.size))
            if (verificationTarget is PluginVerificationTarget.IDE) {
              append(" in the target IDE " + verificationTarget.ideVersion.asString())
            }
          }
        }
        if (compatibilityWarnings.isNotEmpty()) {
          if (isNotEmpty()) append(". ")
          append(compatibilityWarnings.size)
          append(" compatibility ").append("warning".pluralize(compatibilityWarnings.size))
        }
        if (deprecatedUsages.isNotEmpty()) {
          if (isNotEmpty()) append(". ")
          val scheduledForRemovalNumber = deprecatedUsages.count { it.deprecationInfo.forRemoval }
          val deprecatedUsagesNumber = deprecatedUsages.size - scheduledForRemovalNumber
          if (scheduledForRemovalNumber > 0) {
            append("$scheduledForRemovalNumber ").append("usage".pluralize(scheduledForRemovalNumber)).append(" of scheduled for removal API")
          }
          if (deprecatedUsagesNumber > 0) {
            if (scheduledForRemovalNumber > 0) {
              append(" and ")
            }
            append("$deprecatedUsagesNumber ").append("usage".pluralize(deprecatedUsagesNumber)).append(" of deprecated API")
          }
        }
        if (experimentalApiUsages.isNotEmpty()) {
          if (isNotEmpty()) append(". ")
          append("${experimentalApiUsages.size} ").append("usage".pluralize(experimentalApiUsages.size)).append(" of experimental API")
        }
        if (internalApiUsages.isNotEmpty()) {
          if (isNotEmpty()) append(". ")
          append("${internalApiUsages.size} ").append("usage".pluralize(internalApiUsages.size)).append(" of internal API")
        }
        if (nonExtendableApiUsages.isNotEmpty()) {
          if (isNotEmpty()) append(". ")
          append("${nonExtendableApiUsages.size} non-extendable API usage ").append("violation".pluralize(nonExtendableApiUsages.size))
        }
        if (overrideOnlyMethodUsages.isNotEmpty()) {
          if (isNotEmpty()) append(". ")
          append("${overrideOnlyMethodUsages.size} override-only API usage ").append("violation".pluralize(overrideOnlyMethodUsages.size))
        }
        if (pluginStructureWarnings.isNotEmpty()) {
          if (isNotEmpty()) append(". ")
          append("${pluginStructureWarnings.size} plugin configuration ").append("defect".pluralize(pluginStructureWarnings.size))
        }
      }
  }

  class InvalidPlugin(
    plugin: PluginInfo,
    verificationTarget: PluginVerificationTarget,
    val pluginStructureErrors: Set<PluginStructureError>
  ) : PluginVerificationResult(plugin, verificationTarget) {
    override val verificationVerdict
      get() = "Plugin is invalid: " + pluginStructureErrors.joinToString { it.message }
  }

  class NotFound(
    plugin: PluginInfo,
    verificationTarget: PluginVerificationTarget,
    val notFoundReason: String
  ) : PluginVerificationResult(plugin, verificationTarget) {
    override val verificationVerdict
      get() = "Plugin is not found: $notFoundReason"
  }

  class FailedToDownload(
    plugin: PluginInfo,
    verificationTarget: PluginVerificationTarget,
    val failedToDownloadReason: String
  ) : PluginVerificationResult(plugin, verificationTarget) {
    override val verificationVerdict
      get() = "Failed to download plugin: $failedToDownloadReason"
  }

}