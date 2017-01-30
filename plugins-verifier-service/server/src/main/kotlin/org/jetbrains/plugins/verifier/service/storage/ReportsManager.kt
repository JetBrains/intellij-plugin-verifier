package org.jetbrains.plugins.verifier.service.storage

import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.report.CheckIdeReport
import org.slf4j.LoggerFactory
import java.io.File

/**
 * @author Sergey Patrikeev
 */
object ReportsManager {

  private val LOG = LoggerFactory.getLogger(ReportsManager::class.java)

  @Synchronized
  fun saveReport(file: File): Boolean {
    LOG.info("Saving report from file $file")
    val ideReport: CheckIdeReport = try {
      CheckIdeReport.loadFromFile(file)
    } catch(e: Exception) {
      throw IllegalArgumentException("Report file ${file.name} is invalid", e)
    }
    return saveReport(ideReport)
  }

  @Synchronized
  fun saveReport(ideReport: CheckIdeReport): Boolean {
    LOG.info("Saving report IDE=#${ideReport.ideVersion}")
    val report = FileManager.getFileByName(ideReport.ideVersion.asString(), FileType.REPORT)
    ideReport.saveToFile(report)
    LOG.info("Report saved IDE=#${ideReport.ideVersion}")
    return true
  }

  @Synchronized
  fun listReports(): List<IdeVersion> = FileManager.getFilesOfType(FileType.REPORT).map { IdeVersion.createIdeVersion(it.nameWithoutExtension) }

  @Synchronized
  fun deleteReport(ideVersion: IdeVersion): Boolean {
    LOG.info("Deleting report #$ideVersion")
    val file = FileManager.getFileByName(ideVersion.asString(), FileType.REPORT)
    if (!file.exists()) {
      LOG.info("The is no report for $ideVersion")
      return false
    }
    LOG.info("The report $ideVersion is deleted")
    return file.deleteLogged()
  }

  @Synchronized
  fun getReport(ideVersion: IdeVersion): File? {
    val file = FileManager.getFileByName(ideVersion.asString(), FileType.REPORT)
    if (file.exists()) {
      return file
    }
    return null
  }
}