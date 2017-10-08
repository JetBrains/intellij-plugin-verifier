package com.jetbrains.pluginverifier.parameters.filtering.documented

/**
 * @author Sergey Patrikeev
 */
class DocumentedProblemsParser {

  private companion object {
    const val DELIM = '|'

    const val PLACE_HOLDER = "X"

    fun String.toInternalName(): String = replace('.', '/')

    val pattern2Parser = listOf<Pair<String, (String) -> DocumentedProblem?>>(
        "$PLACE_HOLDER class removed" to { x ->
          DocumentedProblem.ClassRemoved(x.toInternalName())
        },
        "$PLACE_HOLDER method removed" to { x ->
          DocumentedProblem.MethodRemoved(x.substringBeforeLast(".").toInternalName(), x.substringAfterLast("."))
        },
        "$PLACE_HOLDER field removed" to { x ->
          DocumentedProblem.FieldRemoved(x.substringBeforeLast(".").toInternalName(), x.substringAfterLast("."))
        },
        "$PLACE_HOLDER package removed" to { x ->
          DocumentedProblem.PackageRemoved(x.toInternalName())
        },
        "$PLACE_HOLDER abstract method added" to { x ->
          DocumentedProblem.AbstractMethodAdded(x.substringBeforeLast(".").toInternalName(), x.substringAfterLast("."))
        }
    )

    fun extractRawMarkdownWord(markdown: String): String {
      //Matches Markdown links: [some-text](http://example.com)
      if (markdown.matches(Regex("\\[.*]\\(.*\\)"))) {
        return extractRawMarkdownWord(markdown.substringAfter("[").substringBefore("]"))
      }
      //Matches Markdown code: `val x = 5`
      if (markdown.startsWith('`') && markdown.endsWith('`')) {
        return markdown.substring(1, markdown.length - 1)
      }
      return markdown
    }

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

  private fun List<String>.matchesPattern(pattern: List<String>): Boolean =
      size == pattern.size && indices.all { index -> this[index] == pattern[index] || pattern[index] == PLACE_HOLDER }

  private fun String.toWords(): List<String> = split(' ')

  private fun List<String>.extractPlaceHolder(pattern: String): String = this[pattern.toWords().indexOf(PLACE_HOLDER)]

  private fun parseDescription(description: String): DocumentedProblem? {
    val words = description.toWords().map { extractRawMarkdownWord(it) }
    val (pattern, parser) = pattern2Parser.find { (pattern, _) ->
      words.matchesPattern(pattern.toWords())
    } ?: return null
    val placeHolder = words.extractPlaceHolder(pattern)
    return parser(placeHolder)
  }
}