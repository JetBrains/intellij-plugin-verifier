package org.jetbrains.plugins.verifier.service.util

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.repository.IdeRepository
import org.jetbrains.plugins.verifier.service.core.TaskManager
import org.jetbrains.plugins.verifier.service.runners.UploadIdeRunner
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Patrikeev
 */
object IdeListUpdater {

  private val LOG: Logger = LoggerFactory.getLogger(IdeListUpdater::class.java)

  //30 minutes
  private val DOWNLOAD_NEW_IDE_PERIOD: Long = 30

  fun start() {
    Executors.newSingleThreadScheduledExecutor(
        ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("ide-repository-%d")
            .build()
    ).scheduleAtFixedRate({ IdeListUpdater.tick() }, 0, DOWNLOAD_NEW_IDE_PERIOD, TimeUnit.MINUTES)
  }

  private fun tick() {
    LOG.info("It's time to upload new IDE versions to the verifier service")
    val alreadyIdes = IdeFilesManager.ideList()
    val maxVersion = alreadyIdes.max()

    IdeRepository.fetchIndex(false)
        .map { IdeVersion.createIdeVersion(it.version) }
        .filter { maxVersion == null || maxVersion < it }
        .forEach {
          val runner = UploadIdeRunner(it.asString(), false, false)

          val taskId = TaskManager.enqueue(runner)
          LOG.info("Uploading IDE version #$it is enqueued with taskId = #$taskId")
        }
  }


}