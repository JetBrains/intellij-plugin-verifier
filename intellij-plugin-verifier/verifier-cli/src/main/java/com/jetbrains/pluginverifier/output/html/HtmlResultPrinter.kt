/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.output.html

import com.jetbrains.plugin.structure.base.utils.create
import com.jetbrains.plugin.structure.ide.VersionComparatorUtil
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.dependencies.presentation.ResolvedDependenciesGraphPrettyPrinter
import com.jetbrains.pluginverifier.misc.HtmlBuilder
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.output.ResultPrinter
import java.io.StringWriter
import java.io.Writer
import java.nio.file.Files
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class HtmlResultPrinter(
  private val verificationTarget: PluginVerificationTarget,
  private val out: Writer
) : ResultPrinter, AutoCloseable {

  companion object {
    fun create(verificationTarget: PluginVerificationTarget, outputOptions: OutputOptions): HtmlResultPrinter {
      val reportHtmlFile = outputOptions.getTargetReportDirectory(verificationTarget).resolve("report.html")
      val writer = Files.newBufferedWriter(reportHtmlFile.create())
      return HtmlResultPrinter(verificationTarget, writer)
    }
  }

  override fun printResults(results: List<PluginVerificationResult>) {
    val htmlBuilder = HtmlBuilder(out)
    htmlBuilder.doPrintResults(results)
    out.flush()
  }

  override fun close() {
    out.close()
  }

  private fun HtmlBuilder.doPrintResults(results: List<PluginVerificationResult>) {
    html {
      head {
        title("Verification result $verificationTarget")
        script(src = "https://ajax.aspnetcdn.com/ajax/jQuery/jquery-1.9.1.min.js", type = "text/javascript")
        script(src = "https://code.jquery.com/ui/1.9.2/jquery-ui.min.js", type = "text/javascript")
        link(rel = "stylesheet", href = "https://code.jquery.com/ui/1.9.2/themes/base/jquery-ui.css", type = "text/css")
        style(type = "text/css") { unsafe(loadReportCss()) }
      }
      body {
        h2 { +verificationTarget.toString() }
        if (results.isEmpty()) {
          +"No plugins checked"
        } else {
          // Keep at most N (concurrencyLevel) results in memory
          val concurrencyLevel = Runtime.getRuntime().availableProcessors()

          // We're using series of latches, so results of callable M is returned only after M-1 was consumed.
          // That ensures that data is sorted and that no more than `concurrencyLevel` rendered results are held in memory at any time.

          val pool = Executors.newFixedThreadPool(concurrencyLevel)
          try {
            val first = CountDownLatch(1)
            val latch = arrayOf(first)
            val futures = results.groupBy { it.plugin.pluginId }.entries
              .sortedBy { it.key }
              .map { (pluginId, pluginResults) ->
                val prev = latch[0]
                val next = CountDownLatch(1)
                latch[0] = next
                pool.submit(Callable {
                  val rendered = renderPluginResults(pluginResults, pluginId, indent)
                  prev.await() // Waiting till main thread saves result from previous Callable
                  // rendered data stored in array to free memory later
                  arrayOf(rendered) to next
                })
              }

            first.countDown() // unlatch first Callable, so first result would complete
            futures.forEach { future ->
              val (rendered, latch) = future.get()
              unsafe(rendered[0])
              rendered[0] = "" // free memory, otherwise Future will held long string
              latch.countDown() // unlatch next Callable, so next result would complete
            }
          } finally {
            pool.shutdownNow()
          }
        }
        script { unsafe(loadReportScript()) }
      }
    }
  }

  private fun renderPluginResults(pluginResults: List<PluginVerificationResult>, pluginId: String, indent: Int): String {
    val writer = StringWriter()
    HtmlBuilder(writer, initialIndent = indent).apply {
      div(classes = "plugin " + getPluginStyle(pluginResults)) {
        h3 {
          span(classes = "pMarker") { +"    " }
          +pluginId
        }
        div {
          pluginResults
            .sortedWith(compareByDescending(VersionComparatorUtil.COMPARATOR) { it.plugin.version })
            .forEach { printPluginResult(it) }
        }
      }
    }.output.flush()
    writer.buffer.apply { if (endsWith('\n')) deleteCharAt(length - 1) }
    return writer.toString()
  }

  private fun getPluginStyle(results: List<PluginVerificationResult>): String {
    if (results.any { it is PluginVerificationResult.InvalidPlugin }) {
      return "badPlugin"
    }
    val verifiedResults = results.filterIsInstance<PluginVerificationResult.Verified>()
    if (verifiedResults.any { it.hasCompatibilityProblems }) {
      return "pluginHasProblems"
    }
    if (verifiedResults.any { it.hasDirectMissingMandatoryDependencies }) {
      return "missingDeps"
    }
    if (verifiedResults.any { it.hasCompatibilityWarnings }) {
      return "warnings"
    }
    return "pluginOk"
  }

  private fun HtmlBuilder.printPluginResult(result: PluginVerificationResult) {
    val resultStyle = when (result) {
      is PluginVerificationResult.Verified -> when {
        result.hasCompatibilityWarnings -> "warnings"
        result.hasDirectMissingMandatoryDependencies -> "missingDeps"
        result.hasCompatibilityProblems -> "updateHasProblems"
        else -> "updateOk"
      }
      is PluginVerificationResult.InvalidPlugin -> "badPlugin"
      is PluginVerificationResult.NotFound -> "notFound"
      is PluginVerificationResult.FailedToDownload -> "failedToDownload"
    }

    div(classes = "update $resultStyle") {
      h3 {
        printUpdateHeader(result)
      }
      div {
        printProblemsAndWarnings(result)
      }
    }
  }

  private fun HtmlBuilder.printUpdateHeader(result: PluginVerificationResult) {
    span(classes = "uMarker") { +"    " }
    +result.plugin.version
    small { +result.plugin.toString() }
    small { +result.verificationVerdict }
  }

  private fun HtmlBuilder.printProblemsAndWarnings(result: PluginVerificationResult) {
    with(result) {
      when (this) {
        is PluginVerificationResult.InvalidPlugin -> printShortAndFullDescription(pluginStructureErrors.joinToString(), result.plugin.pluginId)
        is PluginVerificationResult.NotFound -> printShortAndFullDescription("Plugin ${result.plugin} is not found in the Repository", notFoundReason)
        is PluginVerificationResult.FailedToDownload -> printShortAndFullDescription("Plugin ${result.plugin} is not downloaded from the Repository", failedToDownloadReason)
        is PluginVerificationResult.Verified -> {
          printShortAndFullDescriptionItems("Compatibility problems", compatibilityProblems) { it.shortDescription to it.fullDescription }
          printShortAndFullDescriptionItems("Compatibility warnings", compatibilityWarnings) { it.shortDescription to it.fullDescription }
          printShortAndFullDescriptionItems("Deprecated API usages", deprecatedUsages) { it.shortDescription to it.fullDescription }
          printShortAndFullDescriptionItems("Experimental API usages", experimentalApiUsages) { it.shortDescription to it.fullDescription }
          printShortAndFullDescriptionItems("Internal API usages", internalApiUsages) { it.shortDescription to it.fullDescription }
          printShortAndFullDescriptionItems("Non-extendable API usages", nonExtendableApiUsages) { it.shortDescription to it.fullDescription }
          printShortAndFullDescriptionItems("Override-only API usages", overrideOnlyMethodUsages) { it.shortDescription to it.fullDescription }
          if (pluginStructureWarnings.isNotEmpty()) {
            printShortAndFullDescription("Plugin structure defects") {
              ul {
                pluginStructureWarnings.forEach {
                  li {
                    +it.message
                  }
                }
              }
            }
          }
          if (directMissingMandatoryDependencies.isNotEmpty()) {
            printShortAndFullDescription("Missing dependencies") {
              directMissingMandatoryDependencies.forEach {
                +it.missingReason
              }
            }
          }
          printShortAndFullDescription("Dependencies used on verification") {
            val graphPresentation = ResolvedDependenciesGraphPrettyPrinter(dependenciesGraph).prettyPresentation()
            pre {
              +graphPresentation
            }
          }
        }
      }
    }
  }

  private fun loadReportScript() = HtmlResultPrinter::class.java.getResource("/reportScript.js").readText()

  private fun loadReportCss() = HtmlResultPrinter::class.java.getResource("/reportCss.css").readText()

  private fun <T> HtmlBuilder.printShortAndFullDescriptionItems(
    title: String,
    items: Set<T>,
    mapper: (T) -> Pair<String, String>
  ) {
    if (items.isEmpty()) {
      return
    }
    p {
      printShortAndFullDescription(title) {
        items
          .map(mapper)
          .sortedBy { it.first }
          .groupBy { it.first }
          .forEach { (shortDesc, fullDescriptions) ->
            val allProblems = fullDescriptions.joinToString(separator = "\n")
            printShortAndFullDescription(shortDesc, allProblems)
          }
      }
    }
  }


  private fun HtmlBuilder.printShortAndFullDescription(shortDescription: String, fullDescription: String) {
    printShortAndFullDescription(shortDescription) {
      +fullDescription
    }
  }

  private fun HtmlBuilder.printShortAndFullDescription(shortDescription: String, fullDescriptionBuilder: HtmlBuilder.() -> Unit) {
    div(classes = "shortDescription") {
      indent()
      +shortDescription
      +" "
      a(href = "#", classes = "detailsLink") {
        +"details"
      }
      div(classes = "longDescription") {
        fullDescriptionBuilder()
      }
    }
  }

}
