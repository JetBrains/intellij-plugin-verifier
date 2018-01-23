package com.jetbrains.pluginverifier.results

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.dependencies.emptyDependenciesGraph
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.VerificationResult.*
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.structure.PluginStructureError
import com.jetbrains.pluginverifier.results.structure.PluginStructureWarning

/**
 * Represents possible outcomes of verifying
 * the [plugin] [PluginInfo] against the [ideVersion].
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
   * IDE build number against which the [plugin] has been verified.
   *
   * _This field is applicable for all the result types._
   */
  lateinit var ideVersion: IdeVersion

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
   * Contains the [compatibility problems] [CompatibilityProblem] of the [plugin] when running in the [IDE] [ideVersion].
   *
   * _This field is applicable only for the [CompatibilityProblems] and [MissingDependencies] result types._
   */
  var problems: Set<CompatibilityProblem> = emptySet()

  /**
   * Contains the reason of a [non-downloadable] [FailedToDownload] or [not found] [NotFound] result.
   *
   * _This field is applicable only for the [NotFound] and [FailedToDownload] result types._
   */
  var reason: String = ""

  /**
   * Contains the plugin's references on the IDE's [deprecated] [DeprecatedApiUsage] API.
   *
   * _This field is applicable only for the [OK], [StructureWarnings], [MissingDependencies], and [CompatibilityProblems] result types._
   */
  var deprecatedUsages: Set<DeprecatedApiUsage> = emptySet()

  /**
   * The [plugin] neither has the structure [errors] [PluginStructureError]
   * nor the structure [warnings] [PluginStructureWarning] nor
   * the [compatibility problems] [CompatibilityProblem] when running in the [IDE] [ideVersion].
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
    override fun toString() = "Found ${pluginStructureWarnings.size} " + "warning".pluralize(pluginStructureWarnings.size)
  }

  /**
   * The [plugin] has some [direct] [directMissingDependencies]
   * [missing dependencies] [com.jetbrains.pluginverifier.dependencies.MissingDependency]
   * that were not found during the verification.
   *
   * Some of the [compatibility problems] [problems] might have been caused by miss of the dependencies.
   * For example, problems of type "class X is unresolved" might have been
   * reported because the class `X` resides in some of the unresolved dependencies.
   *
   * The [pluginStructureWarnings] are the [warnings] [PluginProblem.Level.WARNING]
   * of the plugin's structure that should be fixed.*
   *
   * _The available fields are  [dependenciesGraph], [problems] and [pluginStructureWarnings]
   * and [deprecatedUsages].
   */
  class MissingDependencies : VerificationResult() {
    override fun toString() = "Missing ${directMissingDependencies.size} direct plugins and modules " +
        "dependency".pluralize(directMissingDependencies.size) + " and ${problems.size} " + "problem".pluralize(problems.size)

    val directMissingDependencies: List<MissingDependency>
      get() = dependenciesGraph.verifiedPlugin.missingDependencies
  }

  /**
   * The [plugin] has compatibility [problems] when running in the [IDE] [ideVersion].
   *
   * _The available fields are  [problems], [dependenciesGraph], [pluginStructureWarnings]
   * and [deprecatedUsages]._
   */
  class CompatibilityProblems : VerificationResult() {
    override fun toString() = "Found ${problems.size} compatibility " + "problem".pluralize(problems.size) +
        " and ${pluginStructureWarnings.size} " + "warning".pluralize(pluginStructureWarnings.size)
  }

  /**
   * The [plugin]'s structure is invalid due to [pluginStructureErrors].
   *
   * _The available field is only the [pluginStructureErrors]._
   */
  class InvalidPlugin : VerificationResult() {
    override fun toString() = "Plugin is invalid: ${pluginStructureErrors.joinToString()}"
  }

  /**
   * The [plugin] is not found during the verification by some [reason].
   *
   * _The available field is only the [reason]._
   */
  class NotFound : VerificationResult() {
    override fun toString() = "Plugin is not found: $reason"
  }

  /**
   * The [plugin] is registered in the Plugin Repository database
   * but its file couldn't be obtained by some [reason].
   *
   * _The available field is only the [reason]._
   */
  class FailedToDownload : VerificationResult() {
    override fun toString() = "Failed to download plugin: $reason"
  }

}


