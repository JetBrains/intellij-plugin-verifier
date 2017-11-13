package com.jetbrains.pluginverifier.repository.cleanup

import com.jetbrains.pluginverifier.misc.bytesToMegabytes
import com.jetbrains.pluginverifier.repository.AvailableFile
import com.jetbrains.pluginverifier.repository.DownloadManager
import com.jetbrains.pluginverifier.repository.FreeDiskSpaceWatcher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit

class PluginRepositoryFileSweeper(private val spaceWatcher: FreeDiskSpaceWatcher) : FileSweeper {

  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(PluginRepositoryFileSweeper::class.java)

    val LOCKS_TTL: Long = TimeUnit.HOURS.toMillis(8)
  }

  override fun sweep(downloadManager: DownloadManager) {
    val spaceReport = spaceWatcher.getSpaceReport()
    if (spaceReport.availableSpace < spaceReport.lowSpaceThreshold) {
      LOG.info("It's time to remove unused plugins from cache. Download cache usage: ${spaceReport.usedSpace.bytesToMegabytes()} Mb; " +
          "Estimated available space (Mb): ${spaceReport.availableSpace.bytesToMegabytes()}")

      removeUnusedPlugins(downloadManager)
    }
  }

  private fun warnForgottenLocks(availableFiles: List<AvailableFile>) {
    for (availableFile in availableFiles) {
      availableFile.locks
          .filter { System.currentTimeMillis() - it.lockTime > LOCKS_TTL }
          .forEach { lock -> LOG.warn("Forgotten lock found: $availableFile; lock date = ${Date(lock.lockTime)}") }
    }
  }


  private fun removeUnusedPlugins(downloadManager: DownloadManager) {
    val availableFiles = downloadManager.getAvailableFiles()
    warnForgottenLocks(availableFiles)

    val updatesToDelete = availableFiles
        .filter { it.locks.isEmpty() }
        .sortedByDescending { it.size }
        .map { it.file }

    for (update in updatesToDelete) {
      val spaceReport = spaceWatcher.getSpaceReport()
      if (spaceReport.availableSpace > spaceReport.enoughSpaceThreshold) {
        LOG.info("Enough space after cleanup ${spaceReport.availableSpace.bytesToMegabytes()} Mb > ${spaceReport.enoughSpaceThreshold.bytesToMegabytes()} Mb")
        break
      }
      LOG.info("Deleting unused update $update of size ${update.length().bytesToMegabytes()} Mb")
      downloadManager.remove(update)
    }

    val spaceReport = spaceWatcher.getSpaceReport()
    if (spaceReport.availableSpace < spaceReport.lowSpaceThreshold) {
      LOG.warn("Available space after cleanup is not sufficient! ${spaceReport.availableSpace.bytesToMegabytes()} Mb < ${spaceReport.lowSpaceThreshold.bytesToMegabytes()} Mb")
    }
  }

}