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
import com.jetbrains.pluginverifier.warnings.Warning
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
          script(src = "http://ajax.aspnetcdn.com/ajax/jQuery/jquery-1.9.1.min.js", type = "text/javascript")
          script(src = "http://code.jquery.com/ui/1.9.2/jquery-ui.min.js", type = "text/javascript")
          link(rel = "stylesheet", href = "https://code.jquery.com/ui/1.9.2/themes/base/jquery-ui.css", type = "text/css")
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
    div(classes = "plugin " + getPluginStyle(pluginResults)) {
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

  private fun getPluginStyle(pluginResults: List<Result>): String {
    val verdicts = pluginResults
        .filterNot { isExcluded(PluginIdAndVersion(it.plugin.pluginId, it.plugin.version)) }
        .map { it.verdict }
    if (verdicts.any { it is Verdict.Problems }) {
      return "pluginHasProblems"
    }
    if (verdicts.any { it is Verdict.MissingDependencies }) {
      return "missingDeps"
    }
    if (verdicts.any { it is Verdict.Bad }) {
      return "badPlugin"
    }
    if (verdicts.any { it is Verdict.Warnings }) {
      return "warnings"
    }
    return "pluginOk"
  }

  private fun HtmlBuilder.printPluginVerdict(verdict: Verdict, pluginId: String, plugin: PluginInfo, options: PrinterOptions) {
    val verdictStyle = when (verdict) {
      is Verdict.OK -> "updateOk"
      is Verdict.Warnings -> "warnings"
      is Verdict.MissingDependencies -> "missingDeps"
      is Verdict.Problems -> "updateHasProblems"
      is Verdict.Bad -> "badPlugin"
      is Verdict.NotFound -> "notFound"
    }
    val excludedStyle = if (isExcluded(PluginIdAndVersion(pluginId, plugin.version))) "excluded" else ""
    div(classes = "update $verdictStyle $excludedStyle") {
      h3 {
        printUpdateHeader(plugin, verdict, pluginId)
      }
      div {
        printVerificationResult(verdict, plugin, options)
      }
    }
  }

  private fun HtmlBuilder.printUpdateHeader(plugin: PluginInfo, verdict: Verdict, pluginId: String) {
    span(classes = "uMarker") { +"    " }
    +plugin.version
    small { +if (plugin.updateInfo != null) "(#${plugin.updateInfo.updateId})" else "" }
    small {
      +when (verdict) {
        is Verdict.OK -> "OK"
        is Verdict.Warnings -> "${verdict.warnings.size} " + "warning".pluralize(verdict.warnings.size) + " found"
        is Verdict.Problems -> "${verdict.problems.size} " + "problem".pluralize(verdict.problems.size) + " found"
        is Verdict.MissingDependencies -> "Plugin has " +
            "${verdict.missingDependencies.size} missing " + "dependency".pluralize(verdict.missingDependencies.size) + " and " +
            "${verdict.problems.size} " + "problem".pluralize(verdict.problems.size)
        is Verdict.Bad -> "Plugin is invalid"
        is Verdict.NotFound -> "Plugin $pluginId:${plugin.version} is not found in the Repository"
      }
    }
  }

  private fun HtmlBuilder.printVerificationResult(verdict: Verdict, plugin: PluginInfo, options: PrinterOptions) {
    when (verdict) {
      is Verdict.OK -> +"No problems."
      is Verdict.Warnings -> printWarnings(verdict.warnings)
      is Verdict.Problems -> printProblems(verdict.problems)
      is Verdict.Bad -> printShortAndFullDescription(verdict.pluginProblems.joinToString(), plugin.pluginId)
      is Verdict.NotFound -> +"The plugin $plugin is not found in the Repository"
      is Verdict.MissingDependencies -> printMissingDependenciesResult(verdict, options)
    }
  }

  private fun HtmlBuilder.printMissingDependenciesResult(verdict: Verdict.MissingDependencies, options: PrinterOptions) {
    printProblems(verdict.problems)
    val missingDependencies = verdict.missingDependencies
    printMissingDependencies(missingDependencies.filterNot { it.dependency.isOptional })
    printMissingDependencies(missingDependencies.filter { it.dependency.isOptional && !options.ignoreMissingOptionalDependency(it.dependency) })
  }

  private fun HtmlBuilder.printWarnings(warnings: Set<Warning>) {
    p {
      warnings.forEach {
        +it.toString()
        br()
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

}


