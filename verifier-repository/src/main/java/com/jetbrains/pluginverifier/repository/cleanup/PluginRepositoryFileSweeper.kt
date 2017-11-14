package com.jetbrains.pluginverifier.repository.cleanup

import com.jetbrains.pluginverifier.misc.bytesToMegabytes
import com.jetbrains.pluginverifier.repository.FreeDiskSpaceWatcher
import com.jetbrains.pluginverifier.repository.UpdateId
import com.jetbrains.pluginverifier.repository.files.AvailableFile
import com.jetbrains.pluginverifier.repository.files.FileRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit

class PluginRepositoryFileSweeper(private val spaceWatcher: FreeDiskSpaceWatcher) : FileSweeper<UpdateId> {

  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(PluginRepositoryFileSweeper::class.java)

    val LOCKS_TTL: Long = TimeUnit.HOURS.toMillis(8)
  }

  override fun sweep(fileRepository: FileRepository<UpdateId>) {
    val spaceReport = spaceWatcher.getSpaceReport()
    if (spaceReport.availableSpace < spaceReport.lowSpaceThreshold) {
      LOG.info("It's time to remove unused plugins from cache. Download cache usage: ${spaceReport.usedSpace.bytesToMegabytes()} Mb; " +
          "Estimated available space (Mb): ${spaceReport.availableSpace.bytesToMegabytes()}")

      removeUnusedPlugins(fileRepository)
    }
  }

  private fun warnForgottenLocks(availableFiles: List<AvailableFile<UpdateId>>) {
    for (availableFile in availableFiles) {
      availableFile.registeredLocks
          .filter { System.currentTimeMillis() - it.lockTime > LOCKS_TTL }
          .forEach { lock -> LOG.warn("Forgotten lock found: $availableFile; lock date = ${Date(lock.lockTime)}") }
    }
  }


  //todo: remove the updates using LRU order
  private fun removeUnusedPlugins(downloadManager: FileRepository<UpdateId>) {
    val availableFiles = downloadManager.getAvailableFiles()
    warnForgottenLocks(availableFiles)

    val updatesToDelete = availableFiles
        .filter { it.registeredLocks.isEmpty() }
        .sortedByDescending { it.size }

    for ((key, _, size, _) in updatesToDelete) {
      val spaceReport = spaceWatcher.getSpaceReport()
      if (spaceReport.availableSpace > spaceReport.enoughSpaceThreshold) {
        LOG.info("Enough space after cleanup ${spaceReport.availableSpace.bytesToMegabytes()} Mb > ${spaceReport.enoughSpaceThreshold.bytesToMegabytes()} Mb")
        break
      }
      LOG.info("Deleting unused update $key of size ${size.bytesToMegabytes()} Mb")
      downloadManager.remove(key)
    }

    val spaceReport = spaceWatcher.getSpaceReport()
    if (spaceReport.availableSpace < spaceReport.lowSpaceThreshold) {
      LOG.warn("Available space after cleanup is not sufficient! ${spaceReport.availableSpace.bytesToMegabytes()} Mb < ${spaceReport.lowSpaceThreshold.bytesToMegabytes()} Mb")
    }
  }

}