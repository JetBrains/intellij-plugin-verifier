package com.jetbrains.pluginverifier.filtering.documented

import org.jsoup.Connection
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

class DocumentedProblemsPagesFetcher {

  companion object {
    private val subPageRegex = Regex("api_changes/api_changes_list_20..\\.md")
  }

  fun fetchPages(documentedPageUrl: String): List<String> {
    val mainPage = fetchPage(documentedPageUrl)
    val subPagesLinks = extractSubPagesLinks(mainPage, documentedPageUrl)
    return subPagesLinks
        .map { fetchPage(it) }
        .toList<String>()
  }

  private fun fetchPage(documentedPageUrl: String) =
      Jsoup
          .connect(documentedPageUrl)
          .timeout(TimeUnit.MINUTES.toMillis(5).toInt())
          .method(Connection.Method.GET)
          .execute()
          .body()

  private fun extractSubPagesLinks(mainPage: String, mainPageUrl: String) =
      subPageRegex.findAll(mainPage).map {
        mainPageUrl.substringBeforeLast("/") + "/" + it.value
      }
}