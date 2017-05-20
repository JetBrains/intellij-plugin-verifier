package com.jetbrains.pluginverifier.configurations

import com.google.common.collect.ImmutableMultimap
import com.intellij.structure.ide.Ide
import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.plugin.Plugin
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.dependencies.DefaultDependencyResolver
import com.jetbrains.pluginverifier.dependency.DependencyResolver
import com.jetbrains.pluginverifier.report.CheckIdeReport
import com.jetbrains.pluginverifier.repository.RepositoryManager

/**
 * @author Sergey Patrikeev
 */
class CheckTrunkApiConfiguration : Configuration<CheckTrunkApiParams, CheckTrunkApiResults> {

  private data class ResolveArguments(val dependencyId: String, val isModule: Boolean, val dependentId: String, val dependentVersion: String?)

  private var memory: MutableMap<ResolveArguments, DependencyResolver.Result> = hashMapOf<ResolveArguments, DependencyResolver.Result>()

  private lateinit var params: CheckTrunkApiParams

  private fun getDependencyResolverOfMajorIde() = object : DependencyResolver {
    private val default = DefaultDependencyResolver(params.majorIdeDescriptor.createIdeResult.ide)

    override fun resolve(dependencyId: String, isModule: Boolean, dependent: Plugin): DependencyResolver.Result {
      val result = default.resolve(dependencyId, isModule, dependent)
      memory[ResolveArguments(dependencyId, isModule, dependent.pluginId, dependent.pluginVersion)] = result
      return result
    }
  }

  private fun getDependencyResolverOfCurrentIde() = object : DependencyResolver {
    private val defaultResolver = DefaultDependencyResolver(params.ideDescriptor.createIdeResult.ide)

    override fun resolve(dependencyId: String, isModule: Boolean, dependent: Plugin): DependencyResolver.Result =
        memory[ResolveArguments(dependencyId, isModule, dependent.pluginId, dependent.pluginVersion)] ?: defaultResolver.resolve(dependencyId, isModule, dependent)
  }

  override fun execute(parameters: CheckTrunkApiParams): CheckTrunkApiResults {
    params = parameters
    val majorIdeDescriptor = parameters.majorIdeDescriptor
    val pluginsToCheck = getPluginsToCheck(majorIdeDescriptor.ideVersion)
    val (majorBundled, majorReport) = calcReport(majorIdeDescriptor, pluginsToCheck, getDependencyResolverOfMajorIde())
    val ideDescriptor = parameters.ideDescriptor
    val (currentBundled, currentReport) = calcReport(ideDescriptor, pluginsToCheck, getDependencyResolverOfCurrentIde())
    return CheckTrunkApiResults(majorReport, majorBundled, currentReport, currentBundled)
  }

  private fun getPluginsToCheck(ideVersion: IdeVersion): List<PluginDescriptor> = RepositoryManager
        .getLastCompatibleUpdates(ideVersion)
        .map { PluginDescriptor.ByUpdateInfo(it) }

  private fun getBundledPlugins(ide: Ide): BundledPlugins =
      BundledPlugins(ide.bundledPlugins.map { it.pluginId }.distinct(), ide.bundledPlugins.flatMap { it.definedModules }.distinct())


  private fun calcReport(ideDescriptor: IdeDescriptor, pluginsToCheck: List<PluginDescriptor>, dependencyResolver: DependencyResolver?): Pair<BundledPlugins, CheckIdeReport> {
    val checkIdeParams = CheckIdeParams(ideDescriptor, params.jdkDescriptor, pluginsToCheck, ImmutableMultimap.of(), emptyList(), Resolver.getEmptyResolver(), params.externalClassesPrefixes, params.problemsFilter, dependencyResolver = dependencyResolver)
    val ideReport = CheckIdeConfiguration().execute(checkIdeParams).run { CheckIdeReport.createReport(ideDescriptor.ideVersion, results) }
    return getBundledPlugins(ideDescriptor.createIdeResult.ide) to ideReport
  }

}