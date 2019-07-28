package com.jetbrains.pluginverifier.reporting.ignoring

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.base.utils.writeText
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.common.CollectingReporter
import java.nio.file.Path

/**
 * Collects all [ProblemIgnoredEvent] for all [PluginVerificationTarget]s
 * and saves them to `<verification-home>/<verification-target>/all-ignored-problems.txt` files.
 */
class AllIgnoredProblemsReporter(private val targetDirectoryProvider: (PluginVerificationTarget) -> Path) : Reporter<ProblemIgnoredEvent> {

  private val targetToProblemsCollector = hashMapOf<PluginVerificationTarget, CollectingReporter<ProblemIgnoredEvent>>()

  @Synchronized
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
      val allIgnoredProblems = collectingReporter.allReported
      if (allIgnoredProblems.isNotEmpty()) {
        val ignoredProblemsText = formatManyIgnoredProblems(verificationTarget, allIgnoredProblems)
        val ignoredProblemsFile = targetDirectoryProvider(verificationTarget).resolve("all-ignored-problems.txt")

        ignoredProblemsFile.writeText(ignoredProblemsText)
      }
    }
  }

  companion object {
    fun formatManyIgnoredProblems(
        verificationTarget: PluginVerificationTarget,
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