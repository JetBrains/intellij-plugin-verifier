package com.jetbrains.pluginverifier.output.markdown

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.dymamic.DynamicPluginStatus
import com.jetbrains.pluginverifier.output.ResultPrinter
import java.io.PrintWriter

class MarkdownResultPrinter(private val out: PrintWriter) : ResultPrinter {

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
  printVerificationResult("Plugin structure warnings", pluginStructureWarnings) {
    "" to it.message
  }

  printVerificationResult("Missing dependencies", dependenciesGraph.getDirectMissingDependencies()) {
    "" to "${it.dependency}: ${it.missingReason}"
  }

  printVerificationResult("Compatibility warnings", compatibilityWarnings) {
    it.shortDescription to it.fullDescription
  }

  printVerificationResult("Compatibility problems", compatibilityProblems) {
    it.shortDescription to it.fullDescription
  }

  printVerificationResult("Deprecated API usages", deprecatedUsages) {
    it.shortDescription to it.fullDescription
  }

  printVerificationResult("Experimental API usages", experimentalApiUsages) {
    it.shortDescription to it.fullDescription
  }

  printVerificationResult("Internal API usages", internalApiUsages) {
    it.shortDescription to it.fullDescription
  }

  printVerificationResult("Override-only API usages", overrideOnlyMethodUsages) {
    it.shortDescription to it.fullDescription
  }

  printVerificationResult("Non-extendable API usages", nonExtendableApiUsages) {
    it.shortDescription to it.fullDescription
  }

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

