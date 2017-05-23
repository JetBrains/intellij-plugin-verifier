package com.jetbrains.pluginverifier.output

import com.google.common.io.Resources
import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.api.PluginInfo
import com.jetbrains.pluginverifier.api.Result
import com.jetbrains.pluginverifier.api.Verdict
import com.jetbrains.pluginverifier.configurations.PluginIdAndVersion
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.descriptions.FullDescription
import com.jetbrains.pluginverifier.descriptions.ShortDescription
import com.jetbrains.pluginverifier.misc.VersionComparatorUtil
import com.jetbrains.pluginverifier.misc.create
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.problems.Problem
import java.io.File
import java.io.PrintWriter
import java.nio.charset.Charset

class HtmlPrinter(val ideVersions: List<IdeVersion>,
                  val isExcluded: (PluginIdAndVersion) -> Boolean,
                  val htmlFile: File) : Printer {

  private lateinit var htmlBuilder: HtmlBuilder

  override fun printResults(results: List<Result>, options: PrinterOptions) {
    PrintWriter(htmlFile.create()).use {
      htmlBuilder = HtmlBuilder(it)
      doPrintResults(results, options)
    }
  }

  private fun doPrintResults(results: List<Result>, options: PrinterOptions) {
    htmlBuilder.apply {
      html {
        head {
          title("Verification result of IDE ${ideVersions.joinToString()}")
          script(src = "//code.jquery.com/jquery-1.9.1.js")
          script(src = "//code.jquery.com/ui/1.10.4/jquery-ui.js")
          link(rel = "stylesheet", href = "//code.jquery.com/ui/1.10.4/themes/smoothness/jquery-ui.css")
          style(type = "text/css") { unsafe(loadReportCss()) }
        }
        body {
          h2 { +ideVersions.joinToString() }
          label {
            unsafe("""<input id="problematicOnlyCB" type="checkbox" onchange="if ($('#problematicOnlyCB').is(':checked')) {$('body').addClass('problematicOnly')} else {$('body').removeClass('problematicOnly')}">""")
            +"Show problematic plugins only"
          }
          if (results.isEmpty()) {
            +"No plugins checked"
          } else {
            results.groupBy { it.plugin.pluginId }.forEach { (pluginId, pluginResults) ->
              appendPluginResults(pluginResults, pluginId, options)
            }
          }
          script { unsafe(loadReportScript()) }
        }
      }
    }
  }

  private fun HtmlBuilder.appendPluginResults(pluginResults: List<Result>, pluginId: String, options: PrinterOptions) {
    div(classes = "plugin " + if (pluginHasProblems(pluginResults)) "pluginHasProblem" else "pluginOk") {
      h3 {
        span(classes = "pMarker") { +"    " }
        +pluginId
      }
      div {
        pluginResults
            .filterNot { isExcluded(PluginIdAndVersion(it.plugin.pluginId, it.plugin.version)) }
            .sortedWith(compareBy(VersionComparatorUtil.COMPARATOR, { it.plugin.version }))
            .associateBy({ it.plugin }, { it.verdict })
            .forEach { (plugin, verdict) -> printPluginVerdict(verdict, pluginId, plugin, options) }
      }
    }
  }

  private fun HtmlBuilder.printPluginVerdict(verdict: Verdict, pluginId: String, plugin: PluginInfo, options: PrinterOptions) {
    val verdictStyle = if (verdict is Verdict.OK) "updateOk" else "updateHasProblems"
    val excludedStyle = if (isExcluded(PluginIdAndVersion(pluginId, plugin.version))) "excluded" else ""
    div(classes = "update $verdictStyle $excludedStyle") {
      h3 {
        printUpdateHeader(plugin, verdict, pluginId)
      }
      div {
        printVerdict(verdict, plugin, options)
      }
    }
  }

  private fun HtmlBuilder.printUpdateHeader(plugin: PluginInfo, verdict: Verdict, pluginId: String) {
    span(classes = "uMarker") { +"    " }
    +plugin.version
    small { +if (plugin.updateInfo != null) "(#${plugin.updateInfo.updateId})" else "" }
    small {
      +when (verdict) {
        is Verdict.OK -> ""
        is Verdict.Warnings -> ""
        is Verdict.Problems -> "${verdict.problems.size} " + "problem".pluralize(verdict.problems.size) + " found"
        is Verdict.MissingDependencies -> "Plugin has " +
            "${verdict.missingDependencies.size} missing " + "dependency".pluralize(verdict.missingDependencies.size) + " and " +
            "${verdict.problems.size} " + "problem".pluralize(verdict.problems.size)
        is Verdict.Bad -> "Plugin is invalid"
        is Verdict.NotFound -> "Plugin $pluginId:${plugin.version} is not found in the Repository"
      }
    }
  }

  private fun HtmlBuilder.printVerdict(verdict: Verdict, plugin: PluginInfo, options: PrinterOptions) {
    when (verdict) {
      is Verdict.OK -> {
        +"No problems."
      }
      is Verdict.Warnings -> +"${verdict.warnings.size} ${"warning".pluralize(verdict.warnings.size)}"
      is Verdict.Problems -> {
        printProblems(verdict.problems)
      }
      is Verdict.Bad -> {
        printShortAndFullDescription(verdict.pluginProblems.joinToString(), plugin.pluginId)
      }
      is Verdict.NotFound -> {
        +"The plugin $plugin is not found in the Repository"
      }
      is Verdict.MissingDependencies -> {
        printProblems(verdict.problems)

        val missingDependencies = verdict.missingDependencies
        printMissingDependencies(missingDependencies.filterNot { it.dependency.isOptional })
        printMissingDependencies(missingDependencies.filter { it.dependency.isOptional && !options.ignoreMissingOptionalDependency(it.dependency) })
      }
    }
  }

  private fun HtmlBuilder.printMissingDependencies(nonOptionals: List<MissingDependency>) {
    nonOptionals.forEach { printShortAndFullDescription("missing dependency: $it", it.missingReason) }
  }

  private fun loadReportScript() = Resources.toString(HtmlPrinter::class.java.getResource("/reportScript.js"), Charset.forName("UTF-8"))

  private fun loadReportCss() = Resources.toString(HtmlPrinter::class.java.getResource("/reportCss.css"), Charset.forName("UTF-8"))

  private fun HtmlBuilder.printProblems(problems: Set<Problem>) {
    problems.forEach { createProblemTab(it.getShortDescription(), it.getFullDescription()) }
  }

  private fun HtmlBuilder.createProblemTab(shortDescription: ShortDescription, longDescription: FullDescription) {
    printShortAndFullDescription(shortDescription.toString(), longDescription.toString())
  }

  private fun HtmlBuilder.printShortAndFullDescription(shortDescription: String, fullDescription: String) {
    div(classes = "shortDescription") {
      +shortDescription
      +" "
      a(href = "#", classes = "detailsLink") {
        +"details"
      }
      div(classes = "longDescription") {
        +fullDescription
      }
    }
  }

  private fun pluginHasProblems(pluginResults: List<Result>): Boolean =
      pluginResults
          .filterNot { isExcluded(PluginIdAndVersion(it.plugin.pluginId, it.plugin.version)) }
          .filterNot { it.verdict is Verdict.OK }
          .isNotEmpty()

}


