package com.jetbrains.pluginverifier.configurations

import com.google.common.collect.ImmutableMultimap
import com.intellij.structure.ide.Ide
import com.intellij.structure.plugin.Plugin
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.dependency.DependencyResolver
import com.jetbrains.pluginverifier.report.CheckIdeReport
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.utils.DefaultDependencyResolver
import org.slf4j.LoggerFactory

/**
 * @author Sergey Patrikeev
 */
class CheckTrunkApiConfiguration(val params: CheckTrunkApiParams) : Configuration {

  companion object {
    private val LOG = LoggerFactory.getLogger(CheckTrunkApiConfiguration::class.java)
  }

  override fun execute(): CheckTrunkApiResults {
    val majorIdeDescriptor = params.majorIdeDescriptor

    val pluginsToCheck: List<PluginDescriptor> = try {
      RepositoryManager
          .getLastCompatibleUpdates(majorIdeDescriptor.ideVersion)
          .map { PluginDescriptor.ByUpdateInfo(it) }
    } catch(e: Exception) {
      throw RuntimeException("Unable to fetch the list of plugins compatible with ${majorIdeDescriptor.ideVersion}", e)
    }

    data class ResolveArguments(val dependencyId: String, val isModule: Boolean, val dependentId: String, val dependentVersion: String?)

    val memory = hashMapOf<ResolveArguments, DependencyResolver.Result>()

    val majorResolver = object : DependencyResolver {

      private val default = DefaultDependencyResolver(majorIdeDescriptor.createIdeResult.ide)

      override fun resolve(dependencyId: String, isModule: Boolean, dependent: Plugin): DependencyResolver.Result {
        val result = default.resolve(dependencyId, isModule, dependent)
        memory[ResolveArguments(dependencyId, isModule, dependent.pluginId, dependent.pluginVersion)] = result
        return result
      }
    }

    val (majorBundled, majorReport) = calcReport(majorIdeDescriptor, pluginsToCheck, majorResolver)

    val ideDescriptor = params.ideDescriptor
    val currentResolver = object : DependencyResolver {

      private val fallback = DefaultDependencyResolver(ideDescriptor.createIdeResult.ide)

      override fun resolve(dependencyId: String, isModule: Boolean, dependent: Plugin): DependencyResolver.Result {
        return memory[ResolveArguments(dependencyId, isModule, dependent.pluginId, dependent.pluginVersion)] ?: fallback.resolve(dependencyId, isModule, dependent)
      }
    }

    val (currentBundled, currentReport) = calcReport(ideDescriptor, pluginsToCheck, currentResolver)

    return CheckTrunkApiResults(majorReport, majorBundled, currentReport, currentBundled)
  }

  private fun getBundledPlugins(ide: Ide): BundledPlugins =
      BundledPlugins(ide.bundledPlugins.map { it.pluginId }.distinct(), ide.bundledPlugins.flatMap { it.definedModules }.distinct())


  private fun calcReport(ideDescriptor: IdeDescriptor.ByInstance, pluginsToCheck: List<PluginDescriptor>, dependencyResolver: DependencyResolver?): Pair<BundledPlugins, CheckIdeReport> {
    try {
      val checkIdeParams = CheckIdeParams(ideDescriptor, params.jdkDescriptor, pluginsToCheck, ImmutableMultimap.of(), emptyList(), Resolver.getEmptyResolver(), params.externalClassesPrefixes, params.problemsFilter, dependencyResolver = dependencyResolver)
      val ideReport = CheckIdeConfiguration(checkIdeParams).execute().run { CheckIdeReport.createReport(ideDescriptor.ideVersion, results) }
      return getBundledPlugins(ideDescriptor.createIdeResult.ide) to ideReport
    } catch(e: Exception) {
      LOG.error("Failed to verify the IDE ${ideDescriptor.ideVersion}", e)
      throw e
    }
  }

}