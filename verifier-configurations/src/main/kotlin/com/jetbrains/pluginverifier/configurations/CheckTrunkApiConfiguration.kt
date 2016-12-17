package com.jetbrains.pluginverifier.configurations

import com.google.common.collect.ImmutableMultimap
import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.IdeManager
import com.intellij.structure.domain.Plugin
import com.jetbrains.pluginverifier.api.DependencyResolver
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.misc.deleteLogged
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
    try {
      return doExecute()
    } finally {
      if (params.deleteMajorIdeOnExit) {
        params.majorIdeFile.deleteLogged()
      }
    }

  }

  private fun doExecute(): CheckTrunkApiResults {
    val majorIde: Ide
    try {
      majorIde = IdeManager.getInstance().createIde(params.majorIdeFile)
    } catch(e: Exception) {
      LOG.error("Unable to create major IDE from ${params.majorIdeFile}", e)
      throw e
    }

    val pluginsToCheck: List<PluginDescriptor>
    try {
      pluginsToCheck = RepositoryManager
          .getLastCompatibleUpdates(majorIde.version)
          .map { PluginDescriptor.ByUpdateInfo(it) }
    } catch(e: Exception) {
      throw RuntimeException("Unable to fetch the list of plugins compatible with ${majorIde.version}", e)
    }

    data class ResolveArguments(val dependencyId: String, val isModule: Boolean, val dependentId: String, val dependentVersion: String?)

    val memory = hashMapOf<ResolveArguments, DependencyResolver.Result>()

    val majorResolver = object : DependencyResolver {

      private val default = DefaultDependencyResolver(majorIde)

      override fun resolve(dependencyId: String, isModule: Boolean, dependent: Plugin): DependencyResolver.Result {
        val result = default.resolve(dependencyId, isModule, dependent)
        memory[ResolveArguments(dependencyId, isModule, dependent.pluginId, dependent.pluginVersion)] = result
        return result
      }
    }

    val (majorBundled, majorReport) = calcReport(majorIde, pluginsToCheck, majorResolver)

    val currentResolver = object : DependencyResolver {

      private val fallback = DefaultDependencyResolver(params.ide)

      override fun resolve(dependencyId: String, isModule: Boolean, dependent: Plugin): DependencyResolver.Result {
        return memory[ResolveArguments(dependencyId, isModule, dependent.pluginId, dependent.pluginVersion)] ?: fallback.resolve(dependencyId, isModule, dependent)
      }
    }

    val (currentBundled, currentReport) = calcReport(params.ide, pluginsToCheck, currentResolver)

    return CheckTrunkApiResults(majorReport, majorBundled, currentReport, currentBundled)
  }

  private fun getBundledPlugins(ide: Ide): BundledPlugins =
      BundledPlugins(ide.bundledPlugins.map { it.pluginId }.distinct(), ide.bundledPlugins.flatMap { it.definedModules }.distinct())


  private fun calcReport(ide: Ide, pluginsToCheck: List<PluginDescriptor>, dependencyResolver: DependencyResolver?): Pair<BundledPlugins, CheckIdeReport> {
    try {
      val checkIdeParams = CheckIdeParams(IdeDescriptor.ByInstance(ide), params.jdkDescriptor, pluginsToCheck, ImmutableMultimap.of(), params.vOptions, dependencyResolver = dependencyResolver)
      val ideReport = CheckIdeConfiguration(checkIdeParams).execute().run { CheckIdeReport.createReport(ide.version, vResults) }
      return getBundledPlugins(ide) to ideReport
    } catch(e: Exception) {
      LOG.error("Failed to verify the IDE ${ide.version}", e)
      throw e
    }
  }

}