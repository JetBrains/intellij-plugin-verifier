package com.jetbrains.pluginverifier.results

import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.dependencies.emptyDependenciesGraph
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.misc.pluralizeWithNumber
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.VerificationResult.*
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.experimental.ExperimentalApiUsage
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.structure.PluginStructureError
import com.jetbrains.pluginverifier.results.structure.PluginStructureWarning
import java.io.Serializable

/**
 * Represents possible results of verifying [plugin] against [verificationTarget].
 *
 * The verification result can be one of the following:
 * - [OK]
 * - [StructureWarnings]
 * - [MissingDependencies]
 * - [CompatibilityProblems]
 * - [InvalidPlugin]
 * - [NotFound]
 * - [FailedToDownload]
 */
sealed class VerificationResult : Serializable {

  /**
   * Verified plugin.
   *
   * _This field is applicable for all result types._
   */
  lateinit var plugin: PluginInfo

  /**
   * Target against which the [plugin] has been verified.
   *
   * _This field is applicable for all the result types._
   */
  lateinit var verificationTarget: VerificationTarget

  /**
   * [CompatibilityProblem]s that were manually ignored.
   *
   * _This field is applicable only for the [CompatibilityProblems] and [MissingDependencies] result types._
   */
  var ignoredProblems: Set<CompatibilityProblem> = emptySet()

  /**
   * Holds detailed data on:
   * - versions and relations between the dependent plugins that were used during the verification.
   * - [direct] [DependenciesGraph.verifiedPlugin] and [transitive] [DependenciesGraph.getMissingDependencyPaths] missing dependencies
   * - [reasons] [com.jetbrains.pluginverifier.dependencies.MissingDependency.missingReason] why the dependencies are missing
   *
   * _This field is applicable only for the [OK], [StructureWarnings], [MissingDependencies], and [CompatibilityProblems] result types._
   */
  var dependenciesGraph: DependenciesGraph = emptyDependenciesGraph

  /**
   * Contains the plugin's structure [warnings] [PluginProblem.Level.WARNING] that should be fixed.
   *
   * _This field is applicable only for the [StructureWarnings], [MissingDependencies], and [CompatibilityProblems] result types._
   */
  var pluginStructureWarnings: Set<PluginStructureWarning> = emptySet()

  /**
   * Contains the [invalid] plugin [errors] [PluginProblem.Level.ERROR].
   */
  var pluginStructureErrors: Set<PluginStructureError> = emptySet()

  /**
   * Contains the [compatibility problems] [CompatibilityProblem] of the [plugin] against the [verificationTarget].
   *
   * _This field is applicable only for the [CompatibilityProblems] and [MissingDependencies] result types._
   */
  var compatibilityProblems: Set<CompatibilityProblem> = emptySet()

  /**
   * Contains the reason of a [non-downloadable] [FailedToDownload] result.
   *
   * _This field is applicable only for the [FailedToDownload] result type._
   */
  var failedToDownloadReason: String = ""

  /**
   * Contains the reason of a [not-found] [NotFound] result.
   *
   * _This field is applicable only for the [NotFound] result type._
   */
  var notFoundReason: String = ""

  /**
   * Contains [deprecated] [DeprecatedApiUsage] IDE API usages inside the plugin.
   *
   * _This field is applicable only for the [OK], [StructureWarnings], [MissingDependencies], and [CompatibilityProblems] result types._
   */
  var deprecatedUsages: Set<DeprecatedApiUsage> = emptySet()

  /**
   * Contains [experimental] [ExperimentalApiUsage] API usages in the plugin.
   *
   * _This field is applicable only for the [OK], [StructureWarnings], [MissingDependencies], and [CompatibilityProblems] result types._
   */
  var experimentalApiUsages: Set<ExperimentalApiUsage> = emptySet()

  /**
   * Presentable verification verdict
   */
  abstract val verificationVerdict: String

  final override fun toString() = verificationVerdict

  /**
   * The [plugin] neither has the structure [errors] [PluginStructureError]
   * nor the structure [warnings] [PluginStructureWarning] nor
   * the [compatibility problems] [CompatibilityProblem] against the [verificationTarget].
   *
   * _The available fields are  [dependenciesGraph] and [deprecatedUsages]._
   */
  class OK : VerificationResult() {
    override val verificationVerdict
      get() = "OK"

    companion object {
      private const val serialVersionUID = 0L
    }
  }

  /**
   * The [plugin]'s structure has [pluginStructureWarnings] that should be fixed.
   *
   * _The available fields are  [pluginStructureWarnings], [dependenciesGraph] and [deprecatedUsages]._
   */
  class StructureWarnings : VerificationResult() {
    override val verificationVerdict
      get() = "Found " + "warning".pluralizeWithNumber(pluginStructureWarnings.size)

    companion object {
      private const val serialVersionUID = 0L
    }
  }

  /**
   * The [plugin] has some [direct] [directMissingDependencies]
   * [missing dependencies] [com.jetbrains.pluginverifier.dependencies.MissingDependency]
   * that were not found during the verification.
   *
   * Some [compatibility problems] [compatibilityProblems] might have been caused by miss of the dependencies.
   * For example, problems of type "class X is unresolved" might have been
   * reported because the class `X` resides in an unresolved dependency.
   *
   * The [pluginStructureWarnings] are the [warnings] [PluginProblem.Level.WARNING]
   * of the plugin's structure that should be fixed.*
   *
   * _The available fields are  [dependenciesGraph], [compatibilityProblems] and [pluginStructureWarnings]
   * and [deprecatedUsages].
   */
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

    companion object {
      private const val serialVersionUID = 0L
    }
  }

  /**
   * The [plugin] has [compatibilityProblems] against the [verificationTarget].
   *
   * _The available fields are  [compatibilityProblems], [dependenciesGraph], [pluginStructureWarnings]
   * and [deprecatedUsages]._
   */
  class CompatibilityProblems : VerificationResult() {
    override val verificationVerdict
      get() = "Found ${compatibilityProblems.size} compatibility " + "problem".pluralize(compatibilityProblems.size) +
          " and ${pluginStructureWarnings.size} " + "warning".pluralize(pluginStructureWarnings.size)

    companion object {
      private const val serialVersionUID = 0L
    }
  }

  /**
   * The [plugin]'s structure is invalid due to [pluginStructureErrors].
   *
   * _The available field is only the [pluginStructureErrors]._
   */
  class InvalidPlugin : VerificationResult() {
    override val verificationVerdict
      get() = "Plugin is invalid"

    companion object {
      private const val serialVersionUID = 0L
    }
  }

  /**
   * The [plugin] is not found during the verification by some [failedToDownloadReason].
   *
   * _The available field is only the [failedToDownloadReason]._
   */
  class NotFound : VerificationResult() {
    override val verificationVerdict
      get() = "Plugin is not found"

    companion object {
      private const val serialVersionUID = 0L
    }
  }

  /**
   * The [plugin] is registered in the Plugin Repository database
   * but its file couldn't be obtained by some [failedToDownloadReason].
   *
   * _The available field is only the [failedToDownloadReason]._
   */
  class FailedToDownload : VerificationResult() {
    override val verificationVerdict
      get() = "Failed to download plugin"

    companion object {
      private const val serialVersionUID = 0L
    }
  }

}


