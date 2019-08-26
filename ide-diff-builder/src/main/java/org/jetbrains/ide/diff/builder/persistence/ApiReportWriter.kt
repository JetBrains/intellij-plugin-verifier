package org.jetbrains.ide.diff.builder.persistence

import org.jetbrains.ide.diff.builder.api.ApiReport
import java.nio.file.Path

interface ApiReportWriter {
  fun saveReport(apiReport: ApiReport, reportPath: Path)
}