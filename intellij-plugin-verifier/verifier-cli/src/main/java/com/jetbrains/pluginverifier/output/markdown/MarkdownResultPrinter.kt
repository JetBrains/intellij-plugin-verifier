package com.jetbrains.pluginverifier.output.markdown

import com.jetbrains.plugin.structure.base.utils.create
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.dymamic.DynamicPluginStatus
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.output.ResultPrinter
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalApiUsage
import com.jetbrains.pluginverifier.usages.internal.InternalApiUsage
import com.jetbrains.pluginverifier.usages.nonExtendable.NonExtendableApiUsage
import com.jetbrains.pluginverifier.usages.overrideOnly.OverrideOnlyMethodUsage
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning
import com.jetbrains.pluginverifier.warnings.PluginStructureWarning
import java.io.PrintWriter
import java.nio.file.Files

class MarkdownResultPrinter(private val out: PrintWriter) : ResultPrinter, AutoCloseable {

  companion object {
    fun create(verificationTarget: PluginVerificationTarget, outputOptions: OutputOptions): MarkdownResultPrinter {
      val reportHtmlFile = outputOptions.getTargetReportDirectory(verificationTarget).resolve("report.markdown")
      val writer = PrintWriter(Files.newBufferedWriter(reportHtmlFile.create()))
      return MarkdownResultPrinter(writer)
    }
  }

  override fun printResults(results: List<PluginVerificationResult>) {
    markdown(out) {
      results.forEach {
        printResult(it, this)
      }
    }
  }

  private fun printResult(pluginVerificationResult: PluginVerificationResult, markdown: Markdown) {
    with(pluginVerificationResult) {
      markdown.h1("Plugin $plugin against $verificationTarget")
      markdown.paragraph(verificationVerdict)
      if (this is PluginVerificationResult.Verified) {
        markdown + this
      }
    }
  }

  override fun close() {
    out.close()
  }

}

fun markdown(out: PrintWriter, init: Markdown.() -> Unit): Markdown {
  val markdown = Markdown(out)
  markdown.init()
  return markdown
}

class Markdown(private val out: PrintWriter) {
  fun h1(header: String) {
    out.println("# $header")
    out.println()
  }

  fun h2(header: String): Markdown {
    out.println("## $header")
    out.println()
    return this
  }

  fun h3(header: String) {
    out.println("### $header")
    out.println()
  }

  fun unorderedListItem(content: String) {
    out.println("* $content")
  }

  fun unorderedListEnd() {
    out.println()
  }

  fun paragraph(content: String): Markdown {
    out.println(content)
    out.appendLine()
    return this
  }

  operator fun String.unaryPlus() {
    out.println(this)
  }

  operator fun plus(value: String) {
    out.println(value)
  }

  operator fun plus(@Suppress("UNUSED_PARAMETER") markdown: Markdown) {
    // no-op. Concatenating two Markdown instances is equivalent to concatenating their Writers
  }
}

private operator fun Markdown.plus(result: PluginVerificationResult.Verified) {
  printVerificationResult(result)
}

private fun Markdown.printVerificationResult(result: PluginVerificationResult.Verified) = with(result) {
  printVerificationResult("Plugin structure warnings", pluginStructureWarnings, PluginStructureWarning::describe)
  printVerificationResult("Missing dependencies", dependenciesGraph.getDirectMissingDependencies(), MissingDependency::describe)
  printVerificationResult("Compatibility warnings", compatibilityWarnings, CompatibilityWarning::describe)
  printVerificationResult("Compatibility problems", compatibilityProblems, CompatibilityProblem::describe)
  printVerificationResult("Deprecated API usages", deprecatedUsages, DeprecatedApiUsage::describe)
  printVerificationResult("Experimental API usages", experimentalApiUsages, ExperimentalApiUsage::describe)
  printVerificationResult("Internal API usages", internalApiUsages, InternalApiUsage::describe)
  printVerificationResult("Override-only API usages", overrideOnlyMethodUsages, OverrideOnlyMethodUsage::describe)
  printVerificationResult("Non-extendable API usages", nonExtendableApiUsages, NonExtendableApiUsage::describe)

  val dynaStatus = "Dynamic Plugin Status"
  when (val dynamicPluginStatus = dynamicPluginStatus) {
    is DynamicPluginStatus.MaybeDynamic -> h2(dynaStatus) + paragraph("Plugin can probably be enabled or disabled without IDE restart")
    is DynamicPluginStatus.NotDynamic -> h2(dynaStatus) + paragraph("Plugin probably cannot be enabled or disabled without IDE restart: " + dynamicPluginStatus.reasonsNotToLoadUnloadWithoutRestart.joinToString())
    null -> Unit
  }
}

private fun <T> Markdown.printVerificationResult(title: String,
                                                 items: Set<T>, descriptionPropertyExtractor: (T) -> Pair<String, String>) {
  if (items.isNotEmpty()) {
    h2("$title (${items.size}): ")
    val shortToFullDescriptions = items.map(descriptionPropertyExtractor)
      .groupBy({ it.first }, { it.second })
    appendShortAndFullDescriptions(shortToFullDescriptions)
  }
}

private fun Markdown.appendShortAndFullDescriptions(shortToFullDescriptions: Map<String, List<String>>) {
  shortToFullDescriptions.forEach { (shortDescription, fullDescriptions) ->
    if (shortDescription.isNotEmpty()) {
      h3(shortDescription)
    }
    for (fullDescription in fullDescriptions) {
      fullDescription.lines().forEach { line ->
        unorderedListItem(line)
      }
    }
    unorderedListEnd()
  }
}

fun PluginStructureWarning.describe() = "" to message
fun MissingDependency.describe() = "" to "$dependency: $missingReason"
fun CompatibilityWarning.describe() = shortDescription to fullDescription
fun CompatibilityProblem.describe() = shortDescription to fullDescription
fun DeprecatedApiUsage.describe() = shortDescription to fullDescription
fun ExperimentalApiUsage.describe() = shortDescription to fullDescription
fun InternalApiUsage.describe() = shortDescription to fullDescription
fun OverrideOnlyMethodUsage.describe() = shortDescription to fullDescription
fun NonExtendableApiUsage.describe() = shortDescription to fullDescription
