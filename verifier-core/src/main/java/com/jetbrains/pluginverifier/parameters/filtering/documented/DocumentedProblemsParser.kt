package com.jetbrains.pluginverifier.parameters.filtering.documented

/**
 * Parser of the markdown-formatted [Breaking API Changes page](http://www.jetbrains.org/intellij/sdk/docs/reference_guide/api_changes_list.html).
 *
 * @author Sergey Patrikeev
 */
class DocumentedProblemsParser {

  private companion object {
    const val DELIM = '|'

    const val IDENTIFIER = "[\\w.()]+"

    val pattern2Parser = mapOf<Regex, (List<String>) -> DocumentedProblem>(
        Regex("($IDENTIFIER) class removed") to { s -> DocClassRemoved(s[0].toInternalName()) },
        Regex("($IDENTIFIER)\\.($IDENTIFIER) method removed") to { s -> DocMethodRemoved(s[0].toInternalName(), s[1]) },
        Regex("($IDENTIFIER)\\.($IDENTIFIER) field removed") to { s -> DocFieldRemoved(s[0].toInternalName(), s[1]) },
        Regex("($IDENTIFIER) package removed") to { s -> DocPackageRemoved(s[0].toInternalName()) },
        Regex("($IDENTIFIER)\\.($IDENTIFIER) abstract method added") to { s -> DocAbstractMethodAdded(s[0].toInternalName(), s[1]) },
        Regex("($IDENTIFIER) class moved to package ($IDENTIFIER)") to { s -> DocClassMovedToPackage(s[0].toInternalName(), s[1].toInternalName()) }
    )

    /**
     * Gets rid of the markdown code quotes and links
     */
    fun unwrapMarkdownFeatures(markdownWord: String): String {
      //Matches Markdown links: [some-text](http://example.com)
      if (markdownWord.matches(Regex("\\[.*]\\(.*\\)"))) {
        return unwrapMarkdownFeatures(markdownWord.substringAfter("[").substringBefore("]"))
      }
      //Matches Markdown code: `val x = 5`
      if (markdownWord.startsWith('`') && markdownWord.endsWith('`')) {
        return markdownWord.substring(1, markdownWord.length - 1)
      }
      return markdownWord
    }

    fun String.toInternalName(): String = replace('.', '/')
  }

  fun parse(pageBody: String): List<DocumentedProblem> = pageBody.lineSequence()
      .map { it.trim() }
      /**
       * Matches column definition lines like
       * | a | b |
       */
      .filter { it.startsWith(DELIM) && it.endsWith(DELIM) && it.count { it == DELIM } == 3 }
      /**
       * Extracts content of the first column
       */
      .map { it.substring(1, it.length - 1).split(DELIM) }
      .filter { it.size == 2 && it[0].isNotBlank() }
      .map { it[0].trim() }
      /**
       * Parses DocumentedProblem by the column's text
       */
      .mapNotNull { parseDescription(it) }
      .toList()

  private fun parseDescription(text: String): DocumentedProblem? {
    val unwrappedMarkdown = text.split(' ').joinToString(" ") { unwrapMarkdownFeatures(it) }
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