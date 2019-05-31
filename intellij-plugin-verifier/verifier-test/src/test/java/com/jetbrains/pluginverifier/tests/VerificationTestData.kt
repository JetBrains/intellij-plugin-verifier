package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.utils.listRecursivelyAllFilesWithExtension
import org.junit.Assert

data class DescriptionHolder(val shortDescription: String, val fullDescription: String) {
  override fun toString() = buildString {
    appendln(shortDescription)
    appendln(fullDescription)
  }
}

fun parseExpectedProblems(): Sequence<DescriptionHolder> =
    parseDescriptions("PROBLEM")

fun parseExpectedDeprecated(): Sequence<DescriptionHolder> =
    parseDescriptions("DEPRECATED")

fun parseExpectedExperimental(): Sequence<DescriptionHolder> =
    parseDescriptions("EXPERIMENTAL")

fun parseInternalApiUsages(): Sequence<DescriptionHolder> =
    parseDescriptions("INTERNAL")

fun parseOverrideOnlyUsages(): Sequence<DescriptionHolder> =
    parseDescriptions("OVERRIDE_ONLY")

fun parseNonExtendable(): Sequence<DescriptionHolder> =
    parseDescriptions("NON_EXTENDABLE")

private val descriptionRegex = Regex("(.*?)\n\n(.*)", RegexOption.DOT_MATCHES_ALL)

private fun parseDescriptions(type: String): Sequence<DescriptionHolder> =
    sourceFiles()
        .flatMap { expectedBlocks(it, type) }
        .map { block -> parseDescription(block) }

private fun parseDescription(block: String): DescriptionHolder {
  val matchResult = descriptionRegex.matchEntire(block)
  Assert.assertNotNull("Cannot be parsed:\n------\n$block\n------", matchResult)
  return DescriptionHolder(matchResult!!.groupValues[1], matchResult.groupValues[2])
}

private fun sourceFiles(): Sequence<String> =
    findMockPluginSourcePath().toFile()
        .listRecursivelyAllFilesWithExtension("java")
        .asSequence()
        .map { it.readText() }

private fun expectedBlocks(sourceCode: String, expectedType: String): Sequence<String> {
  val regex = Regex("/\\*expected\\($expectedType\\)(.*?)\\*/", RegexOption.DOT_MATCHES_ALL)
  return regex.findAll(sourceCode).map { it.groupValues[1].trimIndent() }
}
