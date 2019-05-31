package com.jetbrains.pluginverifier.results

import com.jetbrains.plugin.structure.base.utils.pluralize
import com.jetbrains.plugin.structure.base.utils.pluralizeWithNumber
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.dependencies.emptyDependenciesGraph
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.structure.PluginStructureError
import com.jetbrains.pluginverifier.results.structure.PluginStructureWarning
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalApiUsage
import com.jetbrains.pluginverifier.usages.internal.InternalApiUsage
import com.jetbrains.pluginverifier.usages.nonExtendable.NonExtendableApiUsage
import com.jetbrains.pluginverifier.usages.overrideOnly.OverrideOnlyMethodUsage

/**
 * Represents possible results of verifying a plugin against an IDE or other plugin.
 */
sealed class VerificationResult : Cloneable {

  lateinit var plugin: PluginInfo
  lateinit var verificationTarget: VerificationTarget

  val pluginStructureWarnings: MutableSet<PluginStructureWarning> = hashSetOf()
  val pluginStructureErrors: MutableSet<PluginStructureError> = hashSetOf()
  val compatibilityProblems: MutableSet<CompatibilityProblem> = hashSetOf()
  val deprecatedUsages: MutableSet<DeprecatedApiUsage> = hashSetOf()
  val experimentalApiUsages: MutableSet<ExperimentalApiUsage> = hashSetOf()
  val internalApiUsages: MutableSet<InternalApiUsage> = hashSetOf()
  val nonExtendableApiUsages: MutableSet<NonExtendableApiUsage> = hashSetOf()
  val overrideOnlyMethodUsages: MutableSet<OverrideOnlyMethodUsage> = hashSetOf()
  var dependenciesGraph: DependenciesGraph = emptyDependenciesGraph
  var failedToDownloadReason: String? = null
  var failedToDownloadError: Throwable? = null
  var notFoundReason: String? = null

  abstract val verificationVerdict: String

  final override fun toString() = verificationVerdict

  class OK : VerificationResult() {
    override val verificationVerdict
      get() = "OK"
  }

  class StructureWarnings : VerificationResult() {
    override val verificationVerdict
      get() = "Found " + "warning".pluralizeWithNumber(pluginStructureWarnings.size)
  }

  class MissingDependencies : VerificationResult() {
    override val verificationVerdict
      get() = {
        val (modules, plugins) = directMissingDependencies.partition { it.dependency.isModule }
        buildString {
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
      }()

    val directMissingDependencies: List<MissingDependency>
      get() = dependenciesGraph.verifiedPlugin.missingDependencies
  }

  class CompatibilityProblems : VerificationResult() {
    override val verificationVerdict
      get() = buildString {
        append("Found ").append(compatibilityProblems.size).append(" compatibility ").append("problem".pluralize(compatibilityProblems.size))
        if (pluginStructureWarnings.isNotEmpty()) {
          append(" and ").append(pluginStructureWarnings.size).append(" ").append("warning".pluralize(pluginStructureWarnings.size))
        }
      }
  }

  class InvalidPlugin : VerificationResult() {
    override val verificationVerdict
      get() = "Plugin is invalid"
  }

  class NotFound : VerificationResult() {
    override val verificationVerdict
      get() = "Plugin is not found: $notFoundReason"
  }

  class FailedToDownload : VerificationResult() {
    override val verificationVerdict
      get() = "Failed to download plugin: $failedToDownloadReason"
  }

}


