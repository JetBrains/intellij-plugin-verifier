package org.jetbrains.plugins.verifier.service.runners

import com.google.common.collect.ImmutableMultimap
import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.IdeManager
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.JdkDescriptor
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.configurations.CheckIdeConfiguration
import com.jetbrains.pluginverifier.configurations.CheckIdeParams
import com.jetbrains.pluginverifier.report.CheckIdeReport
import com.jetbrains.pluginverifier.repository.RepositoryManager
import org.jetbrains.plugins.verifier.service.core.Progress
import org.jetbrains.plugins.verifier.service.core.Task
import org.jetbrains.plugins.verifier.service.params.CheckTrunkApiRunnerParams
import org.jetbrains.plugins.verifier.service.results.CheckTrunkApiResults
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager
import org.jetbrains.plugins.verifier.service.storage.JdkManager
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
      val majorBuildLock: IdeFilesManager.IdeLock? = firstMajorBuild(ide.version.baselineVersion)
      if (majorBuildLock == null) {
        val msg = "There is no major IDE update on the Server with which to compare check results"
        LOG.error(msg)
        throw IllegalStateException(msg)
      }

      try {
        val pluginsToCheck: List<PluginDescriptor> = RepositoryManager
            .getInstance()
            .getLastCompatibleUpdates(majorBuildLock.ide.version)
            .map { PluginDescriptor.ByUpdateInfo(it.pluginId ?: "", it.version ?: "", it) }

        val jdkDescriptor = JdkDescriptor.ByFile(JdkManager.getJdkHome(runnerParams.jdkVersion))
        val majorDescriptor = IdeDescriptor.ByInstance(majorBuildLock.ide)
        val currentDescriptor = IdeDescriptor.ByInstance(ide)

        val majorParams = CheckIdeParams(majorDescriptor, jdkDescriptor, pluginsToCheck, ImmutableMultimap.of(), runnerParams.vOptions)
        val currentParams = majorParams.copy(ideDescriptor = currentDescriptor)

        LOG.debug("${presentableName()} major arguments: $majorParams, current arguments: $currentParams")

        try {
          val majorResults = CheckIdeConfiguration(majorParams).execute()
          val currentResults = CheckIdeConfiguration(currentParams).execute()
          return CheckTrunkApiResults(CheckIdeReport.createReport(majorResults.ideVersion, majorResults.vResults), CheckIdeReport.createReport(currentResults.ideVersion, currentResults.vResults))
        } catch (ie: InterruptedException) {
          throw ie
        } catch (e: Exception) {
          LOG.error("Failed to verify plugins", e)
          throw e
        }

      } finally {
        majorBuildLock.release()
      }
    } finally {
      if (deleteOnCompletion) {
        ideFile.deleteLogged()
      }
    }
  }


  private fun firstMajorBuild(baselineNumber: Int): IdeFilesManager.IdeLock? {
    val major: IdeVersion? = IdeFilesManager
        .ideList()
        .filter { it.baselineVersion == baselineNumber }
        .sorted()
        .firstOrNull()
    if (major != null) {
      return IdeFilesManager.getIde(major)
    }
    return null
  }

}