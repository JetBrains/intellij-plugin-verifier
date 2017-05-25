package org.jetbrains.plugins.verifier.service.runners

import com.google.common.collect.ImmutableMultimap
import com.intellij.structure.ide.Ide
import com.intellij.structure.ide.IdeManager
import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.JdkDescriptor
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.configurations.CheckIdeConfiguration
import com.jetbrains.pluginverifier.configurations.CheckIdeParams
import com.jetbrains.pluginverifier.configurations.CheckTrunkApiResults
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.repository.RepositoryManager
import org.jetbrains.plugins.verifier.service.core.*
import org.jetbrains.plugins.verifier.service.params.CheckTrunkApiRunnerParams
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager
import org.jetbrains.plugins.verifier.service.storage.JdkManager
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Sergey Patrikeev
 */
class CheckTrunkApiRunner(val ideFile: File,
                          val deleteOnCompletion: Boolean,
                          val runnerParams: CheckTrunkApiRunnerParams) : Task<CheckTrunkApiResults>() {
  override fun presentableName(): String = "CheckTrunkApi"

  private val MAX_TIME_DOWNLOADING_MISSING_IDE_MS: Long = 1000 * 60 * 60 // 1 hour

  private val SLEEP_TIME_WAITING_MS: Long = 1000 * 10 //10 minutes

  companion object {
    private val LOG = LoggerFactory.getLogger(CheckTrunkApiResults::class.java)
  }

  override fun computeResult(progress: Progress): CheckTrunkApiResults {
    try {
      val ide: Ide
      try {
        ide = IdeManager.getInstance().createIde(ideFile)
      } catch (e: Exception) {
        throw IllegalArgumentException("The supplied IDE $ideFile is broken", e)
      }

      var majorIdeVersion: IdeVersion
      try {
        majorIdeVersion = IdeVersion.createIdeVersion(runnerParams.majorVersion)
      } catch (e: Exception) {
        try {
          majorIdeVersion = IdeVersion.createIdeVersion(runnerParams.majorVersion.substringAfter("-"))
        } catch (ignore: Exception) {
          throw IllegalArgumentException("Incorrect major IDE version ${runnerParams.majorVersion}", e)
        }
      }

      var majorLock = IdeFilesManager.getIde(majorIdeVersion)
      if (majorLock == null) {
        LOG.info("There is no IDE of version $majorIdeVersion; downloading from the repository")

        val uploadIdeRunner = UploadIdeRunner(majorIdeVersion)

        val downloadFinished = AtomicBoolean()

        val downloadId = TaskManager.enqueue(uploadIdeRunner, {}, { _, _, _ -> }, { _, _ -> downloadFinished.set(true) })

        val startTime = System.currentTimeMillis()

        while (!downloadFinished.get()) {
          LOG.debug("Waiting for the IDE #$majorIdeVersion to be loaded")
          if (System.currentTimeMillis() - startTime > MAX_TIME_DOWNLOADING_MISSING_IDE_MS) {
            throw RuntimeException("Too long waiting (${MAX_TIME_DOWNLOADING_MISSING_IDE_MS / 1000 / 60} minutes) " +
                "for the IDE #$majorIdeVersion to be loaded; download taskId = $downloadId")
          }
          Thread.sleep(SLEEP_TIME_WAITING_MS)
        }

        majorLock = IdeFilesManager.getIde(majorIdeVersion)
        if (majorLock == null) {
          val status = TaskManager.get(downloadId)
          throw IllegalStateException("Unable to download the IDE #$majorIdeVersion; error message is - ${status?.errorMessage}")
        }
      }

      try {
        val pluginsToCheck: List<PluginDescriptor> = try {
          RepositoryManager.getLastCompatibleUpdates(ide.version).map { PluginDescriptor.ByUpdateInfo(it) }
        } catch(e: Exception) {
          throw RuntimeException("Unable to fetch list of plugins compatible with ${ide.version}", e)
        }

        val jdkDescriptor = JdkDescriptor(JdkManager.getJdkHome(runnerParams.jdkVersion))

        var firstPart: Boolean = true

        val halfProgress: Progress = object : DefaultProgress() {

          override fun setProgress(value: Double) = if (firstPart) progress.setProgress(value / 2) else progress.setProgress(0.5 + value / 2)

          override fun getProgress(): Double = progress.getProgress()

          override fun getText(): String = progress.getText()

          override fun setText(text: String) = progress.setText(text)
        }

        val majorReport = calcReport(ide, jdkDescriptor, pluginsToCheck, halfProgress)

        firstPart = false

        val currentReport = calcReport(majorLock.ide, jdkDescriptor, pluginsToCheck, halfProgress)

        return CheckTrunkApiResults(majorReport.first, majorReport.second, currentReport.first, currentReport.second)
      } finally {
        majorLock.release()
      }
    } finally {
      if (deleteOnCompletion) {
        ideFile.deleteLogged()
      }
    }
  }

  private fun calcReport(ide: Ide,
                         jdkDescriptor: JdkDescriptor,
                         pluginsToCheck: List<PluginDescriptor>,
                         progress: Progress): Pair<CheckIdeReport, BundledPlugins> {
    try {
      val params = CheckIdeParams(IdeDescriptor.ByInstance(ide), jdkDescriptor, pluginsToCheck, ImmutableMultimap.of(), runnerParams.vOptions, progress = BridgeVProgress(progress))
      LOG.debug("Check IDE arguments: $params")
      val bundledPlugins = getBundledPlugins(ide)
      val ideReport = CheckIdeConfiguration(params).execute().run { CheckIdeReport.createReport(ide.version, vResults) }
      return ideReport to bundledPlugins
    } catch (ie: InterruptedException) {
      throw ie
    } catch (e: Exception) {
      LOG.error("Unable to verify IDE $ide", e)
      throw e
    }

  }

  private fun getBundledPlugins(ide: Ide): BundledPlugins = BundledPlugins(ide.bundledPlugins.map { it.pluginId }.distinct(), ide.bundledPlugins.flatMap { it.definedModules }.distinct())

}