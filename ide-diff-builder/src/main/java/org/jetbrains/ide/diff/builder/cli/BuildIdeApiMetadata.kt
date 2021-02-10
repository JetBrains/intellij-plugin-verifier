/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.ide.diff.builder.cli

import com.jetbrains.plugin.structure.base.utils.ExecutorWithProgress
import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.base.utils.deleteLogged
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.AvailableIde
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.misc.retry
import com.jetbrains.pluginverifier.repository.files.FileLock
import org.jetbrains.ide.diff.builder.api.ApiEvent
import org.jetbrains.ide.diff.builder.api.ApiReport
import org.jetbrains.ide.diff.builder.api.ApiSignature
import org.jetbrains.ide.diff.builder.filter.ClassFilter
import org.jetbrains.ide.diff.builder.ide.IdeDiffBuilder
import org.jetbrains.ide.diff.builder.persistence.json.JsonApiReportReader
import org.jetbrains.ide.diff.builder.persistence.json.JsonApiReportWriter
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

class BuildIdeApiMetadata {

  companion object {
    private val LOG = LoggerFactory.getLogger("build-ide-api-metadata")

    private val ioThreadCounter = AtomicInteger()
    private val ioExecutor = Executors.newCachedThreadPool { runnable ->
      Thread(runnable).apply {
        isDaemon = true
        name = "io-${ioThreadCounter.incrementAndGet()}"
      }
    }

    /**
     * IDE diff building consumes a lot of file descriptors because many .jar files (IDE libraries, plugins) are being opened.
     * We might get 'IOException: Too many open files' if we ran too many IDE diff buildings in parallel.
     * So let's run IDE diff building in a single application thread to avoid IO errors.
     */
    private val ideDiffThreadCounter = AtomicInteger()
    private val ideDiffExecutor = Executors.newSingleThreadExecutor { runnable ->
      Thread(runnable).apply {
        isDaemon = true
        name = "ide-diff-${ideDiffThreadCounter.incrementAndGet()}"
      }
    }
  }

  fun buildMetadata(
    idesToProcess: List<AvailableIde>,
    ideFilesBank: IdeFilesBank,
    jdkPath: Path,
    classFilter: ClassFilter,
    resultsDirectory: Path
  ): ApiReport {
    require(idesToProcess.size > 1) { "Too few IDE builds to process: ${idesToProcess.size}" }

    val diffsPath = resultsDirectory.resolve("diffs")
    if (System.getProperty("ide.diff.builder.rebuild").orEmpty().equals("true", true)) {
      LOG.info("Removing all existing IDE diffs from $diffsPath")
      diffsPath.deleteLogged()
    }

    val sortedIdesToProcess = idesToProcess.sortedBy { it.version }
    LOG.info("Building IDE diffs for ${sortedIdesToProcess.size} adjacent IDEs: " + sortedIdesToProcess.joinToString())

    val ideDiffBuilder = IdeDiffBuilder(classFilter, jdkPath)
    val ideDiffs = buildAdjacentIdeDiffs(sortedIdesToProcess, ideFilesBank, diffsPath, ideDiffBuilder)

    LOG.info("Merging all IDE diffs into API metadata")
    var metadata: ApiReport = JsonApiReportReader().readApiReport(ideDiffs.first().reportPath)
    for (ideDiff in ideDiffs.drop(1)) {
      val apiReport = JsonApiReportReader().readApiReport(ideDiff.reportPath)
      metadata = mergeApiReports(ideDiff.newIde.version, metadata, apiReport)
    }
    return metadata
  }

  private fun mergeApiReports(
    resultIdeVersion: IdeVersion,
    beforeReport: ApiReport,
    afterReport: ApiReport
  ): ApiReport {
    val signatureToEvents = hashMapOf<ApiSignature, MutableSet<ApiEvent>>()
    val reports = listOf(beforeReport, afterReport)
    for (report in reports) {
      for ((signature, _) in report.asSequence()) {
        if (!signatureToEvents.containsKey(signature)) {
          signatureToEvents[signature] = reports.flatMapTo(hashSetOf()) { it[signature] }
        }
      }
    }
    return ApiReport(resultIdeVersion, signatureToEvents, beforeReport.theFirstIdeVersion, beforeReport.theFirstIdeDeprecatedApis)
  }

  private fun buildAdjacentIdeDiffs(
    idesToProcess: List<AvailableIde>,
    ideFilesBank: IdeFilesBank,
    diffsPath: Path,
    ideDiffBuilder: IdeDiffBuilder
  ): List<IdeDiff> {
    val tasks = (1 until idesToProcess.size).map { index ->
      val previousIde = idesToProcess[index - 1]
      val currentIde = idesToProcess[index]
      ExecutorWithProgress.Task(
        "IDE diff between ${previousIde.version} and ${currentIde.version}",
        BuildIdeDiffTask(
          diffsPath = diffsPath,
          ideFilesBank = ideFilesBank,
          previousIde = previousIde,
          currentIde = currentIde,
          ideDiffBuilder = ideDiffBuilder,
          shouldBuildOldIdeDeprecatedApis = index == 1
        )
      )
    }
    val executor = ExecutorWithProgress<IdeDiff>("ide-diff-builder", 8, false) { progressData ->
      val message = buildString {
        append("Finished ${progressData.finishedNumber} of ${progressData.totalNumber} tasks: ")
        val result = progressData.result
        if (result != null) {
          append("${result.oldIde} against ${result.newIde}")
        } else {
          append("${progressData.exception!!.message}")
        }
      }
      LOG.info(message)
    }
    return executor.executeTasks(tasks).sortedBy { it.oldIde.version }
  }

  private class BuildIdeDiffTask(
    private val diffsPath: Path,
    private val ideFilesBank: IdeFilesBank,
    private val previousIde: AvailableIde,
    private val currentIde: AvailableIde,
    private val ideDiffBuilder: IdeDiffBuilder,
    private val shouldBuildOldIdeDeprecatedApis: Boolean
  ) : Callable<IdeDiff> {
    override fun call(): IdeDiff {
      LOG.info("Building IDE diff between $previousIde and $currentIde")

      val apiReportWriter = JsonApiReportWriter()
      val reportPath = diffsPath.resolve("${previousIde.version}-vs-${currentIde.version}.json")
      if (reportPath.exists()) {
        LOG.info("IDE diff between $previousIde and $currentIde is already built")
      } else {
        val apiReport = buildIdeDiffBetweenIdes(previousIde, currentIde, ideFilesBank, ideDiffBuilder)
        LOG.info("Saving IDE diff between $previousIde and $currentIde to $reportPath")
        apiReportWriter.saveReport(apiReport, reportPath)
      }
      return IdeDiff(reportPath, previousIde, currentIde)
    }

    private fun buildIdeDiffBetweenIdes(
      oldIde: AvailableIde,
      newIde: AvailableIde,
      ideFilesBank: IdeFilesBank,
      ideDiffBuilder: IdeDiffBuilder
    ): ApiReport {
      val oldIdeTask = ideFilesBank.downloadIdeAsync(oldIde)
      val newIdeTask = ideFilesBank.downloadIdeAsync(newIde)

      val (oldIdeFile, oldException) = oldIdeTask.getOrException()
      val (newIdeFile, newException) = newIdeTask.getOrException()

      try {
        if (oldException != null) throw oldException
        if (newException != null) throw newException
        return ideDiffExecutor.submit(Callable {
          ideDiffBuilder.buildIdeDiff(oldIdeFile!!.file, newIdeFile!!.file, shouldBuildOldIdeDeprecatedApis)
        }).get()
      } finally {
        oldIdeFile.closeLogged()
        newIdeFile.closeLogged()
      }
    }

    private fun <T> Future<T>.getOrException(): Pair<T?, Throwable?> =
      try {
        get() to null
      } catch (e: ExecutionException) {
        null to e.cause!!
      } catch (e: Throwable) {
        null to e
      }

    private fun IdeFilesBank.downloadIdeAsync(ide: AvailableIde): Future<FileLock> =
      ioExecutor.submit<FileLock> { downloadIde(ide) }

    private fun IdeFilesBank.downloadIde(ide: AvailableIde): FileLock =
      retry("Download $ide") {
        when (val ideFile = getIdeFile(ide.version)) {
          is IdeFilesBank.Result.Found -> ideFile.ideFileLock
          is IdeFilesBank.Result.NotFound -> throw IllegalArgumentException("$ide is not found: ${ideFile.reason}")
          is IdeFilesBank.Result.Failed -> throw IllegalArgumentException("$ide couldn't be downloaded: ${ideFile.reason}", ideFile.exception)
        }
      }
  }

  private data class IdeDiff(
    val reportPath: Path,
    val oldIde: AvailableIde,
    val newIde: AvailableIde
  )

}