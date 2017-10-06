package com.jetbrains.pluginverifier.reporting.problems

import com.jetbrains.pluginverifier.reporting.common.FileReporter
import com.jetbrains.pluginverifier.results.problems.Problem
import java.io.File

class FileProblemReporter(file: File) : FileReporter<Problem>(file), ProblemReporter {
  override fun reportProblem(problem: Problem) {
    super<FileReporter>.report(problem)
  }

  override fun report(t: Problem) = reportProblem(t)

}