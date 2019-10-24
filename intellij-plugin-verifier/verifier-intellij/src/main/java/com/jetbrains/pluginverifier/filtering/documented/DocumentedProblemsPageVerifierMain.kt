package com.jetbrains.pluginverifier.filtering.documented

/**
 * Main method that checks that all documented problems on page
 * http://www.jetbrains.org/intellij/sdk/docs/reference_guide/api_changes_list.html
 * can be parsed and recognized.
 */
object DocumentedProblemsPageVerifierMain {
  @JvmStatic
  fun main(args: Array<String>) {
    val documentedPages = DocumentedProblemsPagesFetcher().fetchPages()
    val documentedProblemsParser = DocumentedProblemsParser(false)
    for (page in documentedPages) {
      val pageDescriptor = buildString {
        appendln("Source page URL: ${page.sourcePageUrl}")
        appendln("Web page URL: ${page.webPageUrl}")
        appendln("Edit page URL: ${page.editPageUrl}")
      }

      val documentedProblems = try {
        documentedProblemsParser.parse(page.pageBody)
      } catch (e: DocumentedProblemsParseException) {
        throw RuntimeException(
          buildString {
            appendln(pageDescriptor)
            appendln("Failed to parse documented problems page")
            appendln()
            appendln(e.message)
          }
        )
      }

      println("The following documented problems have been successfully parsed from page")
      println(pageDescriptor)
      for (documentedProblem in documentedProblems.sortedBy { it.javaClass.name }) {
        println(documentedProblem)
      }
      println()
    }
  }
}