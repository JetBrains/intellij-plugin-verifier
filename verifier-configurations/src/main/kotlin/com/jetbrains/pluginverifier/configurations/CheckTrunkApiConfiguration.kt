package com.jetbrains.pluginverifier.configurations

import com.google.common.collect.ImmutableMultimap
import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.IdeManager
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.report.CheckIdeReport
import com.jetbrains.pluginverifier.repository.RepositoryManager
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
          .getLastCompatibleUpdates(params.ide.version)
          .map { PluginDescriptor.ByUpdateInfo(it) }
    } catch(e: Exception) {
      throw RuntimeException("Unable to fetch the list of plugins compatible with ${params.ide.version}", e)
    }

    val majorReport = calcReport(majorIde, pluginsToCheck)
    val currentReport = calcReport(params.ide, pluginsToCheck)

    return CheckTrunkApiResults(majorReport.second, majorReport.first, currentReport.second, currentReport.first)
  }

  private fun getBundledPlugins(ide: Ide): BundledPlugins =
      BundledPlugins(ide.bundledPlugins.map { it.pluginId }.distinct(), ide.bundledPlugins.flatMap { it.definedModules }.distinct())


  private fun calcReport(ide: Ide, pluginsToCheck: List<PluginDescriptor>): Pair<BundledPlugins, CheckIdeReport> {
    try {
      val checkIdeParams = CheckIdeParams(IdeDescriptor.ByInstance(ide), params.jdkDescriptor, pluginsToCheck, ImmutableMultimap.of(), params.vOptions)
      val ideReport = CheckIdeConfiguration(checkIdeParams).execute().run { CheckIdeReport.createReport(ide.version, vResults) }
      return getBundledPlugins(ide) to ideReport
    } catch(e: Exception) {
      LOG.error("Failed to verify the IDE ${ide.version}", e)
      throw e
    }
  }

}