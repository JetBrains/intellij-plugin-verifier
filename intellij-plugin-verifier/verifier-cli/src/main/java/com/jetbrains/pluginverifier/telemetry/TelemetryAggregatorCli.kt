package com.jetbrains.pluginverifier.telemetry

import com.jetbrains.pluginverifier.telemetry.parsing.TargetReportDirectoryWalker
import com.jetbrains.pluginverifier.telemetry.parsing.VerificationResultHandler
import java.io.File
import java.nio.file.Path

fun main(args: Array<String>) {
  require(args.isNotEmpty()) {
    "Path to the verification output directory must be set"
  }

  val path = Path.of(args.first())

  val verificationResultHandler = when {
    args.size > 1 -> {
      val csvOutputFile = File(args[1])
      CsvVerificationResultHandler(csvOutputFile)
    }

    else -> object : VerificationResultHandler {}
  }

  TargetReportDirectoryWalker(path).walk(verificationResultHandler)

  if (verificationResultHandler is AutoCloseable) {
    verificationResultHandler.close()
  }
}