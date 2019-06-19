package com.jetbrains.pluginverifier.parameters.filtering.documented

/**
 * Parser of the markdown-formatted [Breaking API Changes page](https://www.jetbrains.org/intellij/sdk/docs/reference_guide/api_changes_list.html).
 */
class DocumentedProblemsParser {

  companion object {
    private const val COLUMNS_DELIMITER = '|'

    private const val METHOD_PARAMS = "\\([^\\)]*\\)"

    private const val IDENTIFIER = "[\\w.$]+"

    private const val S = "[.|#]"

    private val pattern2Parser = mapOf<Regex, (List<String>) -> DocumentedProblem>(
        Regex("($IDENTIFIER) class removed") to { s -> DocClassRemoved(s[0].toInternalName()) },
        Regex("($IDENTIFIER) class renamed.*") to { s -> DocClassRemoved(s[0].toInternalName()) },
        Regex("($IDENTIFIER)$S($IDENTIFIER)($METHOD_PARAMS)? method removed") to { s -> DocMethodRemoved(s[0].toInternalName(), s[1]) },
        Regex("($IDENTIFIER)($METHOD_PARAMS)? constructor removed") to { s -> DocMethodRemoved(s[0].toInternalName(), "<init>") },
        Regex("($IDENTIFIER)$S($IDENTIFIER)($METHOD_PARAMS)? method return type changed.*") to { s -> DocMethodReturnTypeChanged(s[0].toInternalName(), s[1]) },
        Regex("($IDENTIFIER)$S($IDENTIFIER)($METHOD_PARAMS)? method parameter.*(type changed|removed).*") to { s -> DocMethodParameterTypeChanged(s[0].toInternalName(), s[1]) },
        Regex("($IDENTIFIER)($METHOD_PARAMS)? constructor parameter.*(type changed|removed).*") to { s -> DocMethodParameterTypeChanged(s[0].toInternalName(), "<init>") },
        Regex("($IDENTIFIER)$S($IDENTIFIER)($METHOD_PARAMS)? method visibility changed.*") to { s -> DocMethodVisibilityChanged(s[0].toInternalName(), s[1]) },
        Regex("($IDENTIFIER)$S($IDENTIFIER)($METHOD_PARAMS)? method marked final.*") to { s -> DocMethodMarkedFinal(s[0].toInternalName(), s[1]) },
        Regex("($IDENTIFIER).*(class|interface) now (extends|implements) ($IDENTIFIER) and inherits its final method ($IDENTIFIER)($METHOD_PARAMS)?.*") to { s -> DocFinalMethodInherited(s[0].toInternalName(), s[3].toInternalName(), s[4]) },
        Regex("($IDENTIFIER)($METHOD_PARAMS)? constructor visibility changed.*") to { s -> DocMethodVisibilityChanged(s[0].toInternalName(), "<init>") },
        Regex("($IDENTIFIER)$S($IDENTIFIER) field removed") to { s -> DocFieldRemoved(s[0].toInternalName(), s[1]) },
        Regex("($IDENTIFIER)$S($IDENTIFIER) field type changed.*") to { s -> DocFieldTypeChanged(s[0].toInternalName(), s[1]) },
        Regex("($IDENTIFIER)$S($IDENTIFIER) field visibility changed.*") to { s -> DocFieldVisibilityChanged(s[0].toInternalName(), s[1]) },
        Regex("($IDENTIFIER) package removed") to { s -> DocPackageRemoved(s[0].toInternalName()) },
        Regex("($IDENTIFIER)$S($IDENTIFIER)($METHOD_PARAMS)? abstract method added") to { s -> DocAbstractMethodAdded(s[0].toInternalName(), s[1]) },
        Regex("($IDENTIFIER) class moved to package ($IDENTIFIER)") to { s -> DocClassMovedToPackage(s[0].toInternalName(), s[1].toInternalName()) }
    )

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

    /**
     * Converts a presentable class name to the JVM internal name
     * (with dots replaced with /-slashes and $-dollars for inner/nested classes)
     * Examples:
     * - org.some.Class -> org/some/Class
     * - com.example.Inner.Class -> com/example/Inner$Class
     */
    private fun String.toInternalName(): String {
      val idx = indexOfFirst { it.isUpperCase() }
      return if (idx == -1) {
        replace('.', '/')
      } else {
        take(idx).replace('.', '/') + drop(idx).replace('.', '$')
      }
    }
  }

  fun parse(pageBody: String): List<DocumentedProblem> {
    val documentedProblems = arrayListOf<DocumentedProblem>()
    val lines = pageBody.lines()
    for (index in lines.indices) {
      if (lines[index].startsWith(": ") && index > 0) {
        val documentedProblem = parseDescription(lines[index - 1].trim())
        if (documentedProblem != null) {
          documentedProblems += documentedProblem
        }
      }
    }
    return documentedProblems
  }

  private fun parseDescription(text: String): DocumentedProblem? {
    val unwrappedMarkdown = unwrapMarkdownTags(text)
    for ((pattern, parser) in pattern2Parser) {
      val matchResult = pattern.matchEntire(unwrappedMarkdown)
      if (matchResult != null) {
        val values = matchResult.groupValues.drop(1)
        return parser(values)
      }
    }
    return null
  }
}