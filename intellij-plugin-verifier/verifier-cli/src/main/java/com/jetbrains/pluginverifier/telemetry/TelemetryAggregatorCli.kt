package com.jetbrains.pluginverifier.telemetry

import com.jetbrains.pluginverifier.telemetry.parsing.TargetReportDirectoryWalker
import com.jetbrains.pluginverifier.telemetry.parsing.VerificationResultHandler
import kotlin.io.path.Path

fun main(args: Array<String>) {
  require(args.isNotEmpty()) {
    "Path to the verification output directory must be set"
  }

  val targetReportDirectory = Path(args.first())
  val verificationResultHandler = when {
    args.size > 1 -> {
      val csvOutputPath = Path(args[1])
      CsvVerificationResultHandler(csvOutputPath)
    }

    else -> object : VerificationResultHandler {}
  }

  TargetReportDirectoryWalker(targetReportDirectory).walk(verificationResultHandler)

  if (verificationResultHandler is AutoCloseable) {
    verificationResultHandler.close()
  }
}