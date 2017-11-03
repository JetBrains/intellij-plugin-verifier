package com.jetbrains.pluginverifier.parameters.filtering.documented

/**
 * Parser of the markdown-formatted [Breaking API Changes page](http://www.jetbrains.org/intellij/sdk/docs/reference_guide/api_changes_list.html).
 *
 * @author Sergey Patrikeev
 */
class DocumentedProblemsParser {

  companion object {
    private const val COLUMNS_DELIMITER = '|'

    private val METHOD_PARAMS = "\\([^\\)]*\\)"

    private val IDENTIFIER = "[\\w.$]+"

    //todo: don't ignore the method parameter types.
    private val pattern2Parser = mapOf<Regex, (List<String>) -> DocumentedProblem>(
        Regex("($IDENTIFIER) class removed") to { s -> DocClassRemoved(s[0].toInternalName()) },
        Regex("($IDENTIFIER)\\.($IDENTIFIER)($METHOD_PARAMS)? method removed") to { s -> DocMethodRemoved(s[0].toInternalName(), s[1]) },
        Regex("($IDENTIFIER)\\.($IDENTIFIER)($METHOD_PARAMS)? method return type changed.*") to { s -> DocMethodReturnTypeChanged(s[0].toInternalName(), s[1]) },
        Regex("($IDENTIFIER)\\.($IDENTIFIER)($METHOD_PARAMS)? method parameter type changed.*") to { s -> DocMethodParameterTypeChanged(s[0].toInternalName(), s[1]) },
        Regex("($IDENTIFIER)\\.($IDENTIFIER)($METHOD_PARAMS)? method visibility changed.*") to { s -> DocMethodVisibilityChanged(s[0].toInternalName(), s[1]) },
        Regex("($IDENTIFIER)\\.($IDENTIFIER) field removed") to { s -> DocFieldRemoved(s[0].toInternalName(), s[1]) },
        Regex("($IDENTIFIER)\\.($IDENTIFIER) field type changed.*") to { s -> DocFieldTypeChanged(s[0].toInternalName(), s[1]) },
        Regex("($IDENTIFIER)\\.($IDENTIFIER) field visibility changed.*") to { s -> DocFieldVisibilityChanged(s[0].toInternalName(), s[1]) },
        Regex("($IDENTIFIER) package removed") to { s -> DocPackageRemoved(s[0].toInternalName()) },
        Regex("($IDENTIFIER)\\.($IDENTIFIER) abstract method added") to { s -> DocAbstractMethodAdded(s[0].toInternalName(), s[1]) },
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

    private fun String.toInternalName(): String = replace('.', '/')
  }

  fun parse(pageBody: String): List<DocumentedProblem> = pageBody.lineSequence()
      .map { it.trim() }
      /**
       * Matches column definition lines like
       * | a | b |
       */
      .filter { it.startsWith(COLUMNS_DELIMITER) && it.endsWith(COLUMNS_DELIMITER) && it.count { it == COLUMNS_DELIMITER } == 3 }
      /**
       * Extracts content of the first column
       */
      .map { it.substring(1, it.length - 1).split(COLUMNS_DELIMITER) }
      .filter { it.size == 2 && it[0].isNotBlank() }
      .map { it[0].trim() }
      /**
       * Parses DocumentedProblem by the column's text
       */
      .mapNotNull { parseDescription(it) }
      .toList()

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