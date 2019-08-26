package org.jetbrains.ide.diff.builder.persistence

import org.jetbrains.ide.diff.builder.api.ApiReport
import java.nio.file.Path

interface ApiReportReader {
  fun readApiReport(reportPath: Path): ApiReport
}