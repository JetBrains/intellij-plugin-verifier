/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.filtering.documented

import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.verifiers.VerificationContext

/**
 * Parser of the markdown-formatted [Breaking API Changes page](https://www.jetbrains.org/intellij/sdk/docs/reference_guide/api_changes_list.html).
 */
class DocumentedProblemsParser(private val ignoreNonParsed: Boolean) {

  companion object {

    private const val METHOD_PARAMS = "\\([^\\)]*\\)"

    private const val IDENTIFIER = "[\\w.$]+"

    private const val S = "[.|#]"

    @Suppress("RedundantLambdaArrow")
    private val pattern2Parser = listOf<Pair<Regex, (List<String>) -> DocumentedProblem>>(
      Regex("($IDENTIFIER).*(?:class|interface|annotation|enum) removed") to { s -> DocClassRemoved(toInternalName(s[0])) },
      Regex("($IDENTIFIER).*(?:class|interface|annotation|enum) renamed.*") to { s -> DocClassRemoved(toInternalName(s[0])) },
      Regex("($IDENTIFIER)$S($IDENTIFIER)($METHOD_PARAMS)? method removed") to { s -> DocMethodRemoved(toInternalName(s[0]), s[1]) },
      Regex("($IDENTIFIER)$S($IDENTIFIER)($METHOD_PARAMS)? method moved.*") to { s -> DocMethodRemoved(toInternalName(s[0]), s[1]) },
      Regex("($IDENTIFIER)($METHOD_PARAMS)? constructor removed") to { s -> DocMethodRemoved(toInternalName(s[0]), "<init>") },
      Regex("($IDENTIFIER)$S($IDENTIFIER)($METHOD_PARAMS)? method return type changed.*") to { s -> DocMethodReturnTypeChanged(toInternalName(s[0]), s[1]) },
      Regex("($IDENTIFIER)$S($IDENTIFIER)($METHOD_PARAMS)? method parameter.*(type changed|removed).*") to { s -> DocMethodParameterTypeChanged(toInternalName(s[0]), s[1]) },
      Regex("($IDENTIFIER)($METHOD_PARAMS)? constructor parameter.*(type changed|removed).*") to { s -> DocMethodParameterTypeChanged(toInternalName(s[0]), "<init>") },
      Regex("($IDENTIFIER)$S($IDENTIFIER)($METHOD_PARAMS)? method visibility changed.*") to { s -> DocMethodVisibilityChanged(toInternalName(s[0]), s[1]) },
      Regex("($IDENTIFIER)$S($IDENTIFIER)($METHOD_PARAMS)? method marked final.*") to { s -> DocMethodMarkedFinal(toInternalName(s[0]), s[1]) },
      Regex("($IDENTIFIER).*(?:class|interface) now (?:extends|implements) ($IDENTIFIER) and inherits its final method ($IDENTIFIER)($METHOD_PARAMS)?.*") to { s -> DocFinalMethodInherited(toInternalName(s[0]), toInternalName(s[1]), s[2]) },
      Regex("($IDENTIFIER).*(?:class|interface) now (?:extends|implements) ($IDENTIFIER) and inherits its abstract method ($IDENTIFIER)($METHOD_PARAMS)?.*") to { s -> DocAbstractMethodAdded(toInternalName(s[0]), toInternalName(s[2])) },
      Regex("($IDENTIFIER)($METHOD_PARAMS)? constructor visibility changed.*") to { s -> DocMethodVisibilityChanged(toInternalName(s[0]), "<init>") },
      Regex("($IDENTIFIER)$S($IDENTIFIER) field removed") to { s -> DocFieldRemoved(toInternalName(s[0]), s[1]) },
      Regex("($IDENTIFIER)$S($IDENTIFIER) field moved.*") to { s -> DocFieldRemoved(toInternalName(s[0]), s[1]) },
      Regex("($IDENTIFIER)$S($IDENTIFIER) field type changed.*") to { s -> DocFieldTypeChanged(toInternalName(s[0]), s[1]) },
      Regex("($IDENTIFIER)$S($IDENTIFIER) field visibility changed.*") to { s -> DocFieldVisibilityChanged(toInternalName(s[0]), s[1]) },
      Regex("($IDENTIFIER) package removed") to { s -> DocPackageRemoved(toInternalName(s[0])) },
      Regex("($IDENTIFIER)$S($IDENTIFIER)($METHOD_PARAMS)? (abstract method added|marked abstract)") to { s -> DocAbstractMethodAdded(toInternalName(s[0]), s[1]) },
      Regex("($IDENTIFIER).*(?:class|interface|annotation|enum) moved to package ($IDENTIFIER)") to { s -> DocClassMovedToPackage(toInternalName(s[0]), toInternalName(s[1])) },
      Regex("($IDENTIFIER)$S($IDENTIFIER) method ($IDENTIFIER) parameter marked @($IDENTIFIER)") to { s -> DocMethodParameterMarkedWithAnnotation(toInternalName(s[0]), s[1], toInternalName(s[2]), toInternalName(s[3])) },
      Regex("($IDENTIFIER)(.*)type parameter ($IDENTIFIER) added") to { s -> DocClassTypeParameterAdded(toInternalName(s[0])) },
      Regex("($IDENTIFIER).*(?:superclass|superinterface) changed from ($IDENTIFIER) to ($IDENTIFIER)") to { s -> DocSuperclassChanged(toInternalName(s[0]), toInternalName(s[1]), toInternalName(s[2])) },
      Regex("($IDENTIFIER) property removed from resource bundle ($IDENTIFIER)") to { s -> DocPropertyRemoved(s[0], s[1]) }
    )

    /**
     * Converts a presentable class name to the JVM internal name
     * (with dots replaced with /-slashes and $-dollars for inner/nested classes)
     * Examples:
     * - org.some.Class -> org/some/Class
     * - com.example.Inner.Class -> com/example/Inner$Class
     * - com.somePackage.SomeClass -> com/somePackage/SomeClass
     */
    fun toInternalName(dotClassName: String): String {
      val parts = dotClassName.split(".")
      require(parts.all { it.isNotEmpty() }) { "Has empty parts: $dotClassName" }
      val packageName = parts.takeWhile { it.first().isLowerCase() }.joinToString("/")
      val className = parts.dropWhile { it.first().isLowerCase() }.joinToString("$")
      if (packageName.isEmpty()) {
        return className
      }
      if (className.isEmpty()) {
        return packageName
      }
      return "$packageName/$className"
    }

    /**
     * Gets rid of the markdown code quotes and links.
     */
    fun unwrapMarkdownTags(text: String): String {
      //Matches Markdown links: [some-text](http://example.com)

      val markdownLinksRegex = Regex("\\[(.*)]\\(.*\\)")
      var result = text
      while (markdownLinksRegex in result) {
        result = result.replace(markdownLinksRegex, "$1")
      }

      //Matches Markdown code: `val x = 5`
      val codeQuotesRegex = Regex("`(.*)`")
      while (codeQuotesRegex in result) {
        result = result.replace(codeQuotesRegex, "$1")
      }

      return result
    }

    fun startsWithMarkdown(text: String): Boolean =
      text.startsWith("`") || text.startsWith("[`")
  }

  fun parse(pageBody: String): List<DocumentedProblem> {
    val lines = parseLines(pageBody)
    val documentedProblems = arrayListOf<DocumentedProblem>()
    for (index in lines.indices) {
      if (lines[index].startsWith(": ") && index > 0) {
        val description = lines[index - 1].trim()
        if (!startsWithMarkdown(description)) {
          // Ignore human-readable descriptions of non-code changes, like
          // "Constructor injection referring to extension points not supported" or
          // "Java code migrated to use nullability annotations"
          continue
        }
        val unwrappedDescription = unwrapMarkdownTags(description)
        val documentedProblem = parseUnwrappedDescription(unwrappedDescription)
          ?: if (ignoreNonParsed) {
            continue
          } else {
            throw createParseException(description, unwrappedDescription)
          }
        documentedProblems += documentedProblem
      }
    }
    return documentedProblems
  }

  private fun createParseException(description: String, unwrappedDescription: String) =
    DocumentedProblemsParseException(
      buildString {
        appendln("Unable to parse documented problem description")
        appendln("Description: \"$description\"")
        appendln("Description (no Markdown): \"$unwrappedDescription\"")
        appendln("Only the following patterns are supported: ")
        appendln("Where <method-params>='$METHOD_PARAMS' and <identifier>='$IDENTIFIER':")
        for (regex in pattern2Parser.map { it.first }) {
          appendln(
            regex.pattern
              .replace(METHOD_PARAMS, "<method-params>")
              .replace(IDENTIFIER, "<identifier>")
              .replace(S, ".")
          )
        }
      }
    )

  /**
   * ---
   * title: SomeTitle
   * ---
   *
   * <!--
   * comment
   * -->
   *
   * # Header
   * ## Another header
   *
   * `com.something.Changed` class removed
   * : Class `com.something.Changed` class removed
   */
  private fun parseLines(pageBody: String): List<String> {
    val lines = arrayListOf<String>()
    var insideTitle = false
    var insideComment = false
    for (line in pageBody.lineSequence()) {
      if (line.startsWith("#")) {
        continue
      }
      if (line.startsWith("---")) {
        insideTitle = !insideTitle
        continue
      }
      if (insideTitle) {
        continue
      }
      if (line.startsWith("<!--")) {
        insideComment = true
        continue
      }
      if (line.startsWith("-->")) {
        insideComment = false
        continue
      }
      if (insideComment) {
        continue
      }
      lines += line
    }
    return lines
  }

  private fun parseUnwrappedDescription(unwrappedDescription: String): DocumentedProblem? {
    for ((pattern, parser) in pattern2Parser) {
      val matchResult = pattern.matchEntire(unwrappedDescription)
      if (matchResult != null) {
        val values = matchResult.groupValues.drop(1)
        return parser(values)
      }
    }
    return null
  }
}