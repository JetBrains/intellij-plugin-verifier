package com.jetbrains.pluginverifier.results

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
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
 * - [Warnings]
 * - [MissingDependencies]
 * - [Problems]
 * - [InvalidPlugin]
 * - [NotFound]
 * - [FailedToDownload]
 */
sealed class VerificationResult(
    /**
     * Plugin that was verified.
     */
    val plugin: PluginInfo,
    /**
     * IDE build number against which the [plugin] was verified.
     */
    val ideVersion: IdeVersion,
    /**
     * Compatibility [CompatibilityProblem]s that were manually ignored.
     */
    val ignoredProblems: Set<CompatibilityProblem>
) {

  /**
   * The [plugin] neither has the structure [errors] [PluginStructureError]
   * nor the structure [warnings] [PluginStructureWarning] nor
   * the [compatibility problems] [CompatibilityProblem] when running in the [IDE] [ideVersion].
   *
   * The [dependenciesGraph] holds versions and relations between
   * the dependent plugins that were used during the [plugin] verification.
   *
   * The [deprecatedUsages] contain the plugin's references during the IDE [deprecated] [DeprecatedApiUsage] API.
   */
  class OK(pluginInfo: PluginInfo,
           ideVersion: IdeVersion,
           ignoredProblems: Set<CompatibilityProblem>,
           val dependenciesGraph: DependenciesGraph,
           val deprecatedUsages: Set<DeprecatedApiUsage>) : VerificationResult(pluginInfo, ideVersion, ignoredProblems) {
    override fun toString() = "OK"
  }

  /**
   * The [plugin]'s structure has [pluginStructureWarnings] that should be fixed.
   *
   * The [dependenciesGraph] holds versions and relations between
   * the dependent plugins that were used during the [plugin] verification.
   *
   * The [deprecatedUsages] contain the plugin's references during the IDE [deprecated] [DeprecatedApiUsage] API.
   */
  class Warnings(pluginInfo: PluginInfo,
                 ideVersion: IdeVersion,
                 ignoredProblems: Set<CompatibilityProblem>,
                 val pluginStructureWarnings: Set<PluginStructureWarning>,
                 val dependenciesGraph: DependenciesGraph,
                 val deprecatedUsages: Set<DeprecatedApiUsage>) : VerificationResult(pluginInfo, ideVersion, ignoredProblems) {
    override fun toString() = "Found ${pluginStructureWarnings.size} " + "warning".pluralize(pluginStructureWarnings.size)
  }

  /**
   * The [plugin] has some direct [dependencies] [com.jetbrains.plugin.structure.intellij.plugin.PluginDependency]
   * that were not found during the verification.
   *
   * The [dependenciesGraph] holds detailed data on:
   * - versions and relations between the dependent plugins that were used during the verification.
   * - [direct] [DependenciesGraph.verifiedPlugin] and [transitive] [DependenciesGraph.getMissingDependencyPaths] missing dependencies
   * - [reasons] [com.jetbrains.pluginverifier.dependencies.MissingDependency.missingReason] why the dependencies are missing
   *
   * The [problems] may contain the compatibility [problems] [CompatibilityProblem],
   * some of which might have been caused by miss of the dependencies.
   * For example, the problems of type "class X is unresolved" might have been
   * reported because the class `X` resides in some of the unresolved dependencies.
   *
   * The [pluginStructureWarnings] are the [warnings] [PluginProblem.Level.WARNING]
   * of the plugin structure that should be fixed.
   *
   * The [deprecatedUsages] contain the plugin's references during the IDE [deprecated] [DeprecatedApiUsage] API.
   */
  class MissingDependencies(pluginInfo: PluginInfo,
                            ideVersion: IdeVersion,
                            ignoredProblems: Set<CompatibilityProblem>,
                            val dependenciesGraph: DependenciesGraph,
                            val problems: Set<CompatibilityProblem>,
                            val pluginStructureWarnings: Set<PluginStructureWarning>,
                            val deprecatedUsages: Set<DeprecatedApiUsage>) : VerificationResult(pluginInfo, ideVersion, ignoredProblems) {
    override fun toString() = "Missing ${directMissingDependencies.size} direct plugins and modules " + "dependency".pluralize(directMissingDependencies.size) + " and ${problems.size} " + "problem".pluralize(problems.size)

    val directMissingDependencies = dependenciesGraph.verifiedPlugin.missingDependencies

  }

  /**
   * The [plugin] has compatibility [problems] when running in the [IDE] [ideVersion].
   *
   * The [dependenciesGraph] holds versions and relations between
   * the dependent plugins that were used during the [plugin] verification.
   *
   * The [pluginStructureWarnings] contain the plugin's structure [pluginStructureWarnings] [PluginProblem.Level.WARNING]
   * that should be fixed.
   *
   * The [deprecatedUsages] contain the plugin's references during the IDE [deprecated] [DeprecatedApiUsage] API.
   */
  class Problems(pluginInfo: PluginInfo,
                 ideVersion: IdeVersion,
                 ignoredProblems: Set<CompatibilityProblem>,
                 val problems: Set<CompatibilityProblem>,
                 val dependenciesGraph: DependenciesGraph,
                 val pluginStructureWarnings: Set<PluginStructureWarning>,
                 val deprecatedUsages: Set<DeprecatedApiUsage>) : VerificationResult(pluginInfo, ideVersion, ignoredProblems) {
    override fun toString() = "Found ${problems.size} compatibility " + "problem".pluralize(problems.size) + " and ${pluginStructureWarnings.size} " + "warning".pluralize(pluginStructureWarnings.size)
  }

  /**
   * The [plugin]'s structure is invalid due to [pluginStructureErros].
   */
  class InvalidPlugin(pluginInfo: PluginInfo,
                      ideVersion: IdeVersion,
                      ignoredProblems: Set<CompatibilityProblem>,
                      val pluginStructureErros: List<PluginStructureError>) : VerificationResult(pluginInfo, ideVersion, ignoredProblems) {
    override fun toString() = "Plugin is invalid: ${pluginStructureErros.joinToString()}"
  }

  /**
   * The [plugin] is not found during the verification by some [reason].
   */
  class NotFound(pluginInfo: PluginInfo,
                 ideVersion: IdeVersion,
                 ignoredProblems: Set<CompatibilityProblem>,
                 val reason: String) : VerificationResult(pluginInfo, ideVersion, ignoredProblems) {
    override fun toString() = "Plugin is not found: $reason"
  }

  /**
   * The [plugin] is registered in the Plugin Repository database
   * but its file couldn't be obtained by some [reason].
   */
  class FailedToDownload(pluginInfo: PluginInfo,
                         ideVersion: IdeVersion,
                         ignoredProblems: Set<CompatibilityProblem>,
                         val reason: String) : VerificationResult(pluginInfo, ideVersion, ignoredProblems)

}


