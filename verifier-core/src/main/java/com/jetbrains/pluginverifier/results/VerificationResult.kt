package com.jetbrains.pluginverifier.results

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.dependencies.emptyDependenciesGraph
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.misc.pluralizeWithNumber
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.VerificationResult.*
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.structure.PluginStructureError
import com.jetbrains.pluginverifier.results.structure.PluginStructureWarning

/**
 * Represents possible outcomes of verifying
 * the [plugin] [PluginInfo] against the [verificationTarget].
 *
 * The verification outcome can be one of the following:
 * - [OK]
 * - [StructureWarnings]
 * - [MissingDependencies]
 * - [CompatibilityProblems]
 * - [InvalidPlugin]
 * - [NotFound]
 * - [FailedToDownload]
 */
sealed class VerificationResult {

  /**
   * Verified plugin.
   *
   * _This field is applicable for all the result types._
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
   * The [plugin] neither has the structure [errors] [PluginStructureError]
   * nor the structure [warnings] [PluginStructureWarning] nor
   * the [compatibility problems] [CompatibilityProblem] against the [verificationTarget].
   *
   * _The available fields are  [dependenciesGraph] and [deprecatedUsages]._
   */
  class OK : VerificationResult() {
    override fun toString() = "OK"
  }

  /**
   * The [plugin]'s structure has [pluginStructureWarnings] that should be fixed.
   *
   * _The available fields are  [pluginStructureWarnings], [dependenciesGraph] and [deprecatedUsages]._
   */
  class StructureWarnings : VerificationResult() {
    override fun toString() = "Found " + "warning".pluralizeWithNumber(pluginStructureWarnings.size)
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
    override fun toString(): String {
      val (modules, plugins) = directMissingDependencies.partition { it.dependency.isModule }
      return buildString {
        append("Missing ")
        if (modules.isNotEmpty()) {
          append(modules.size)
          append(" direct " + "module".pluralize(modules.size))
        }
        if (plugins.isNotEmpty()) {
          if (modules.isNotEmpty()) {
            append(" and ")
          }
          append(plugins.size)
          append(" direct " + "plugin".pluralize(plugins.size))
        }
        append(" ")
        append("dependency".pluralize(modules.size + plugins.size))
        if (compatibilityProblems.isNotEmpty()) {
          append(" and ${compatibilityProblems.size} compatibility " + "problem".pluralize(compatibilityProblems.size))
        }
      }
    }

    val directMissingDependencies: List<MissingDependency>
      get() = dependenciesGraph.verifiedPlugin.missingDependencies
  }

  /**
   * The [plugin] has [compatibilityProblems] against the [verificationTarget].
   *
   * _The available fields are  [compatibilityProblems], [dependenciesGraph], [pluginStructureWarnings]
   * and [deprecatedUsages]._
   */
  class CompatibilityProblems : VerificationResult() {
    override fun toString() = "Found ${compatibilityProblems.size} compatibility " + "problem".pluralize(compatibilityProblems.size) +
        " and ${pluginStructureWarnings.size} " + "warning".pluralize(pluginStructureWarnings.size)
  }

  /**
   * The [plugin]'s structure is invalid due to [pluginStructureErrors].
   *
   * _The available field is only the [pluginStructureErrors]._
   */
  class InvalidPlugin : VerificationResult() {
    override fun toString() = "Plugin is invalid"
  }

  /**
   * The [plugin] is not found during the verification by some [failedToDownloadReason].
   *
   * _The available field is only the [failedToDownloadReason]._
   */
  class NotFound : VerificationResult() {
    override fun toString() = "Plugin is not found"
  }

  /**
   * The [plugin] is registered in the Plugin Repository database
   * but its file couldn't be obtained by some [failedToDownloadReason].
   *
   * _The available field is only the [failedToDownloadReason]._
   */
  class FailedToDownload : VerificationResult() {
    override fun toString() = "Failed to download plugin"
  }

}


