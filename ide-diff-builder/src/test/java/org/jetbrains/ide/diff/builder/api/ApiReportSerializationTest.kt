package org.jetbrains.ide.diff.builder.api

import org.jetbrains.ide.diff.builder.persistence.ApiReportReader
import org.jetbrains.ide.diff.builder.persistence.ApiReportWriter
import org.jetbrains.ide.diff.builder.persistence.json.JsonApiReportReader
import org.jetbrains.ide.diff.builder.persistence.json.JsonApiReportWriter
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class ApiReportSerializationTest : BaseOldNewIdesTest() {

  @Rule
  @JvmField
  val tempFolder = TemporaryFolder()

  @Test
  fun `build, save and read API report as Json`() {
    val apiReport = IdeDiffBuilderTest().buildApiReport()
    val reportPath = tempFolder.newFile("report.json").toPath()
    saveAndRead(apiReport, reportPath, JsonApiReportReader(), JsonApiReportWriter())
  }

  private fun saveAndRead(
    originalReport: ApiReport,
    reportPath: Path,
    apiReportReader: ApiReportReader,
    reportWriter: ApiReportWriter
  ) {
    reportWriter.saveReport(originalReport, reportPath)
    val restoredReport = apiReportReader.readApiReport(reportPath)

    for ((originalSignature, events) in originalReport.apiSignatureToEvents) {
      val restoredEvents = restoredReport[originalSignature]
      assertSetsEqual(events, restoredEvents.toSet())
    }

    assertSetsEqual(originalReport.theFirstIdeDeprecatedApis!!, restoredReport.theFirstIdeDeprecatedApis!!)
  }

}