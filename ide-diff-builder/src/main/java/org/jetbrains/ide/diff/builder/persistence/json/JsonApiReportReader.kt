package org.jetbrains.ide.diff.builder.persistence.json

import org.jetbrains.ide.diff.builder.api.ApiReport
import org.jetbrains.ide.diff.builder.persistence.ApiReportReader
import java.nio.file.Files
import java.nio.file.Path

class JsonApiReportReader : ApiReportReader {
  override fun readApiReport(reportPath: Path): ApiReport =
    jsonInstance.parse(
      ApiReport.serializer(),
      Files.newBufferedReader(reportPath).use { it.readText() }
    )
}