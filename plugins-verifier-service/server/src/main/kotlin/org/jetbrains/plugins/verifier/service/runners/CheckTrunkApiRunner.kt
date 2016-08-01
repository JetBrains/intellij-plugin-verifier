package org.jetbrains.plugins.verifier.service.runners

import com.google.common.collect.ImmutableMultimap
import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.IdeManager
import com.intellij.structure.domain.IdeVersion
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.JdkDescriptor
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.configurations.CheckIdeConfiguration
import com.jetbrains.pluginverifier.configurations.CheckIdeParams
import com.jetbrains.pluginverifier.report.CheckIdeReport
import com.jetbrains.pluginverifier.repository.RepositoryManager
import org.jetbrains.plugins.verifier.service.core.BridgeVProgress
import org.jetbrains.plugins.verifier.service.core.Progress
import org.jetbrains.plugins.verifier.service.core.Task
import org.jetbrains.plugins.verifier.service.params.CheckTrunkApiRunnerParams
import org.jetbrains.plugins.verifier.service.results.CheckTrunkApiResults
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager
import org.jetbrains.plugins.verifier.service.storage.JdkManager
import org.jetbrains.plugins.verifier.service.storage.ReportsManager
import org.jetbrains.plugins.verifier.service.util.deleteLogged
import org.slf4j.LoggerFactory
import java.io.File

/**
 * @author Sergey Patrikeev
 */
class CheckTrunkApiRunner(val ideFile: File,
                          val deleteOnCompletion: Boolean,
                          val runnerParams: CheckTrunkApiRunnerParams) : Task<CheckTrunkApiResults>() {
  override fun presentableName(): String = "CheckTrunkApi"

  companion object {
    private val LOG = LoggerFactory.getLogger(CheckTrunkApiResults::class.java)
  }

  override fun computeImpl(progress: Progress): CheckTrunkApiResults {
    try {
      val ide: Ide
      try {
        ide = IdeManager.getInstance().createIde(ideFile)
      } catch (e: Exception) {
        throw IllegalArgumentException("The supplied IDE $ideFile is broken", e)
      }

      val pluginsToCheck: List<PluginDescriptor>
      try {
        pluginsToCheck = RepositoryManager
            .getInstance()
            .getLastCompatibleUpdates(ide.version)
            .map { PluginDescriptor.ByUpdateInfo(it) }
      } catch(e: Exception) {
        throw RuntimeException("Unable to fetch list of plugins compatible with ${ide.version}", e)
      }

      val jdkDescriptor = JdkDescriptor.ByFile(JdkManager.getJdkHome(runnerParams.jdkVersion))

      val majorVersion = getMajorVersion(ide.version.baselineVersion)
      if (majorVersion == null) {
        val msg = "There is no major IDE update on the Server with which to compare check results"
        LOG.error(msg)
        throw IllegalStateException(msg)
      }

      val majorReport = calculateMajorReport(majorVersion, jdkDescriptor, pluginsToCheck, progress)
      val currentReport = calculateIdeReport(ide, jdkDescriptor, pluginsToCheck, progress)

      ReportsManager.saveReport(majorReport.first)
      ReportsManager.saveReport(currentReport.first)

      return CheckTrunkApiResults(majorReport.first, majorReport.second, currentReport.first, currentReport.second)
    } finally {
      if (deleteOnCompletion) {
        ideFile.deleteLogged()
      }
    }
  }

  private fun getMajorVersion(trunkNumber: Int): IdeVersion? {
    return when (trunkNumber) {
      162 -> {
        val ideVersion = IdeVersion.createIdeVersion(Settings.TRUNK_162_RELEASE_VERSION.get())
        require(ideVersion in IdeFilesManager.ideList())
        ideVersion
      }
      else -> return IdeFilesManager.ideList().filter { it.baselineVersion == trunkNumber }.sorted().firstOrNull()
    }
  }

  private fun calculateIdeReport(ide: Ide,
                                 jdkDescriptor: JdkDescriptor.ByFile,
                                 pluginsToCheck: List<PluginDescriptor>,
                                 progress: Progress): Pair<CheckIdeReport, CheckTrunkApiResults.BundledPlugins> {
    try {
      val currentParams = CheckIdeParams(IdeDescriptor.ByInstance(ide), jdkDescriptor, pluginsToCheck, ImmutableMultimap.of(), runnerParams.vOptions, Resolver.getEmptyResolver(), BridgeVProgress(progress))
      LOG.debug("${presentableName()} current arguments: $currentParams")
      return CheckIdeConfiguration(currentParams).execute().run { CheckIdeReport.createReport(ideVersion, vResults) } to CheckTrunkApiResults.BundledPlugins(ide.bundledPlugins.map { it.pluginId })
    } catch (ie: InterruptedException) {
      throw ie
    } catch (e: Exception) {
      LOG.error("Failed to verify plugins", e)
      throw e
    }
  }

  private fun calculateMajorReport(majorVersion: IdeVersion,
                                   jdkDescriptor: JdkDescriptor.ByFile,
                                   pluginsToCheck: List<PluginDescriptor>,
                                   progress: Progress): Pair<CheckIdeReport, CheckTrunkApiResults.BundledPlugins> {
    val majorBuildLock: IdeFilesManager.IdeLock = IdeFilesManager.getIde(majorVersion)!!
    try {
      val bundledPluginIds = majorBuildLock.ide.bundledPlugins.map { it.pluginId }
      val existingReport = getExistingReport(majorVersion)
      if (existingReport != null) {
        return existingReport to CheckTrunkApiResults.BundledPlugins(bundledPluginIds)
      }
      val majorParams = CheckIdeParams(IdeDescriptor.ByInstance(majorBuildLock.ide), jdkDescriptor, pluginsToCheck, ImmutableMultimap.of(), runnerParams.vOptions, Resolver.getEmptyResolver(), BridgeVProgress(progress))
      LOG.debug("${presentableName()} major arguments: $majorParams")
      return CheckIdeConfiguration(majorParams).execute().run { CheckIdeReport.createReport(ideVersion, vResults) } to CheckTrunkApiResults.BundledPlugins(bundledPluginIds)
    } catch (ie: InterruptedException) {
      throw ie
    } catch (e: Exception) {
      LOG.error("Failed to verify major IDE", e)
      throw e
    } finally {
      majorBuildLock.release()
    }
  }


  private fun getExistingReport(ideVersion: IdeVersion): CheckIdeReport? = ReportsManager.getReport(ideVersion)?.run { CheckIdeReport.loadFromFile(this) }

}