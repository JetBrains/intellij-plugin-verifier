/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.filtering.documented

/**
 * Main method that checks that all documented problems on page
 * http://www.jetbrains.org/intellij/sdk/docs/reference_guide/api_changes_list.html
 * can be parsed and recognized.
 */
object DocumentedProblemsPageVerifierMain {
  @JvmStatic
  fun main(args: Array<String>) {
    println("WARN: CLI arguments are ignored. You can avoid passing them.")

    val (repository, branch) = args + arrayOf("", "")
    val url = "https://raw.githubusercontent.com/$repository/$branch/reference_guide/api_changes_list.md".takeIf {
      repository.isNotBlank() && branch.isNotBlank()
    }
    val documentedPages = DocumentedProblemsPagesFetcher().fetchPages(url)
    check(documentedPages.isNotEmpty()) { "No pages" }
    val documentedProblemsParser = DocumentedProblemsParser(false)
    for (page in documentedPages) {
      val pageDescriptor = buildString {
        appendLine("Source page URL: ${page.sourcePageUrl}")
        appendLine("Web page URL: ${page.webPageUrl}")
        appendLine("Edit page URL: ${page.editPageUrl}")
      }

      val documentedProblems = try {
        documentedProblemsParser.parse(page.pageBody)
      } catch (e: DocumentedProblemsParseException) {
        throw RuntimeException(
          buildString {
            appendLine(pageDescriptor)
            appendLine("Failed to parse documented problems page")
            appendLine()
            appendLine(e.message)
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
