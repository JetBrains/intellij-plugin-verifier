package com.jetbrains.pluginverifier.reporting.ignoring

import com.jetbrains.pluginverifier.reporting.common.FileReporter
import java.io.File

/**
 * @author Sergey Patrikeev
 */
class FileIgnoredProblemReporter(file: File) : FileReporter<String>(file), IgnoredProblemReporter {
  override fun reportIgnoredProblem(ignoredDescription: String) {
    super<FileReporter>.report(ignoredDescription)
  }

  override fun report(t: String) = reportIgnoredProblem(t)
}