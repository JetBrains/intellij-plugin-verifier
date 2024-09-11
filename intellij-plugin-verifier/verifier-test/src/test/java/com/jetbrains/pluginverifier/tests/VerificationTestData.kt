package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.utils.listRecursivelyAllFilesWithExtension
import com.jetbrains.plugin.structure.base.utils.readText
import org.junit.Assert

data class DescriptionHolder(val shortDescription: String, val fullDescription: String, val type: DescriptionType) {
  override fun toString() = buildString {
    appendLine("/*expected(${type.name})")
    appendLine(shortDescription)
    appendLine()
    appendLine(fullDescription)
    appendLine("*/")
  }
}

enum class DescriptionType {
  PROBLEM,
  WARNING,
  DEPRECATED,
  EXPERIMENTAL,
  INTERNAL,
  OVERRIDE_ONLY,
  NON_EXTENDABLE;
}

fun parseExpectedProblems(): Sequence<DescriptionHolder> =
  parseDescriptions(DescriptionType.PROBLEM)

fun parseExpectedWarnings(): Sequence<DescriptionHolder> =
  parseDescriptions(DescriptionType.WARNING)

fun parseExpectedDeprecated(): Sequence<DescriptionHolder> =
  parseDescriptions(DescriptionType.DEPRECATED)

fun parseExpectedExperimental(): Sequence<DescriptionHolder> =
  parseDescriptions(DescriptionType.EXPERIMENTAL)

fun parseInternalApiUsages(): Sequence<DescriptionHolder> =
  parseDescriptions(DescriptionType.INTERNAL)

fun parseOverrideOnlyUsages(): Sequence<DescriptionHolder> =
  parseDescriptions(DescriptionType.OVERRIDE_ONLY)

fun parseNonExtendable(): Sequence<DescriptionHolder> =
  parseDescriptions(DescriptionType.NON_EXTENDABLE)

private val descriptionRegex = Regex("(.*?)\n\n(.*)", RegexOption.DOT_MATCHES_ALL)

private fun parseDescriptions(type: DescriptionType): Sequence<DescriptionHolder> =
  sourceFiles()
    .flatMap { expectedBlocks(it, type) }
    .map { block -> parseDescription(block, type) }

private fun parseDescription(block: String, type: DescriptionType): DescriptionHolder {
  val matchResult = descriptionRegex.matchEntire(block)
  Assert.assertNotNull("Cannot be parsed:\n------\n$block\n------", matchResult)
  return DescriptionHolder(matchResult!!.groupValues[1], matchResult.groupValues[2], type)
}

private fun sourceFiles(): Sequence<String> {
  val javaFiles = findMockPluginSourcePath().listRecursivelyAllFilesWithExtension("java").asSequence()
  val kotlinFiles = listOf(findMockPluginSourcePath(), findMockPluginKotlinSourcePath())
    .flatMap { it.listRecursivelyAllFilesWithExtension("kt") }
    .asSequence()

  return (javaFiles + kotlinFiles).map { it.readText() }
}

private fun expectedBlocks(sourceCode: String, expectedType: DescriptionType): Sequence<String> {
  val regex = Regex("/\\*expected\\(${expectedType.name}\\)(.*?)\\*/", RegexOption.DOT_MATCHES_ALL)
  return regex.findAll(sourceCode).map { it.groupValues[1].trimIndent() }
}
