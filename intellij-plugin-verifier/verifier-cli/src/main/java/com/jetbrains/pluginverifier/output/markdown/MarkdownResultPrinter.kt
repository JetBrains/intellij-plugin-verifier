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
      markdown.h1("Plugin $plugin against $verificationTarget: $verificationVerdict")
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
    out.println("## $header")
    out.println()
  }

  fun unorderedListItem(content: String) {
    out.println("* $content")
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
  result.printVerificationResult(this)
}

private fun PluginVerificationResult.Verified.printVerificationResult(markdown: Markdown) = with(markdown) {
  if (pluginStructureWarnings.isNotEmpty()) {
    h2("Plugin structure warnings (${pluginStructureWarnings.size}): ")
    for (warning in pluginStructureWarnings) {
      unorderedListItem(warning.message)
    }
  }

  val directMissingDependencies = dependenciesGraph.getDirectMissingDependencies()
  if (directMissingDependencies.isNotEmpty()) {
    h2("Missing dependencies: ")
    for (missingDependency in directMissingDependencies) {
      unorderedListItem("${missingDependency.dependency}: ${missingDependency.missingReason}")
    }
  }

  if (compatibilityWarnings.isNotEmpty()) {
    h2("Compatibility warnings (${compatibilityWarnings.size}): ")
    appendShortAndFullDescriptions(compatibilityWarnings.groupBy({ it.shortDescription }, { it.fullDescription }))
  }

  if (compatibilityProblems.isNotEmpty()) {
    h2("Compatibility problems (${compatibilityProblems.size}): ")
    appendShortAndFullDescriptions(compatibilityProblems.groupBy({ it.shortDescription }, { it.fullDescription }))
  }

  if (deprecatedUsages.isNotEmpty()) {
    h2("Deprecated API usages (${deprecatedUsages.size}): ")
    appendShortAndFullDescriptions(deprecatedUsages.groupBy({ it.shortDescription }, { it.fullDescription }))
  }

  if (experimentalApiUsages.isNotEmpty()) {
    h2("Experimental API usages (${experimentalApiUsages.size}): ")
    appendShortAndFullDescriptions(experimentalApiUsages.groupBy({ it.shortDescription }, { it.fullDescription }))
  }

  if (internalApiUsages.isNotEmpty()) {
    h2("Internal API usages (${internalApiUsages.size}): ")
    appendShortAndFullDescriptions(internalApiUsages.groupBy({ it.shortDescription }, { it.fullDescription }))
  }

  if (overrideOnlyMethodUsages.isNotEmpty()) {
    h2("Override-only API usages (${overrideOnlyMethodUsages.size}): ")
    appendShortAndFullDescriptions(overrideOnlyMethodUsages.groupBy({ it.shortDescription }, { it.fullDescription }))
  }

  if (nonExtendableApiUsages.isNotEmpty()) {
    h2("Non-extendable API usages (${nonExtendableApiUsages.size}): ")
    appendShortAndFullDescriptions(nonExtendableApiUsages.groupBy({ it.shortDescription }, { it.fullDescription }))
  }


  val dynaStatus = "Dynamic Plugin Status"
  when (val dynamicPluginStatus = dynamicPluginStatus) {
    is DynamicPluginStatus.MaybeDynamic -> h2(dynaStatus) + paragraph("Plugin can probably be enabled or disabled without IDE restart")
    is DynamicPluginStatus.NotDynamic -> h2(dynaStatus) + paragraph("Plugin probably cannot be enabled or disabled without IDE restart: " + dynamicPluginStatus.reasonsNotToLoadUnloadWithoutRestart.joinToString())
    null -> Unit
  }

}

private fun Markdown.appendShortAndFullDescriptions(shortToFullDescriptions: Map<String, List<String>>) {
  shortToFullDescriptions.forEach { (shortDescription, fullDescriptions) ->
    h3(shortDescription)
    for (fullDescription in fullDescriptions) {
      fullDescription.lines().forEach { line ->
        unorderedListItem(line)
      }
    }
  }
}

