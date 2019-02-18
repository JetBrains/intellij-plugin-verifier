package com.jetbrains.pluginverifier.output.reporters

import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.misc.writeText
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.common.CollectingReporter
import com.jetbrains.pluginverifier.reporting.ignoring.ProblemIgnoredEvent

/**
 * Collects all [ProblemIgnoredEvent] for all [VerificationTarget]s
 * and saves them to `<verification-home>/<verification-target>/all-ignored-problems.txt` files.
 */
class AllIgnoredProblemsReporter(private val outputOptions: OutputOptions) : Reporter<ProblemIgnoredEvent> {

  private val targetToProblemsCollector = hashMapOf<VerificationTarget, CollectingReporter<ProblemIgnoredEvent>>()

  override fun report(t: ProblemIgnoredEvent) {
    targetToProblemsCollector
        .getOrPut(t.verificationTarget) { CollectingReporter() }
        .report(t)
  }

  override fun close() {
    try {
      saveIdeIgnoredProblems()
    } finally {
      targetToProblemsCollector.values.forEach { it.closeLogged() }
    }
  }

  private fun saveIdeIgnoredProblems() {
    for ((verificationTarget, collectingReporter) in targetToProblemsCollector) {
      val allIgnoredProblems = collectingReporter.getReported()
      if (allIgnoredProblems.isNotEmpty()) {
        val ignoredProblemsText = formatManyIgnoredProblems(verificationTarget, allIgnoredProblems)
        val ignoredProblemsFile = outputOptions.getTargetReportDirectory(verificationTarget)
            .resolve("all-ignored-problems.txt")

        ignoredProblemsFile.writeText(ignoredProblemsText)
      }
    }
  }

  companion object {
    fun formatManyIgnoredProblems(
        verificationTarget: VerificationTarget,
        allIgnoredProblems: List<ProblemIgnoredEvent>
    ) = buildString {
      appendln("The following problems against $verificationTarget were ignored:")
      for ((reason, allWithReason) in allIgnoredProblems.groupBy { it.reason }) {
        appendln("because $reason:")
        for ((shortDescription, allWithShortDescription) in allWithReason.groupBy { it.problem.shortDescription }) {
          appendln("    $shortDescription:")
          for ((plugin, allWithPlugin) in allWithShortDescription.groupBy { it.plugin }) {
            appendln("      $plugin:")
            for (ignoredEvent in allWithPlugin) {
              appendln("        ${ignoredEvent.problem.fullDescription}")
            }
          }
          appendln()
        }
      }
    }
  }


}