/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.filtering.documented

import org.jsoup.Connection
import org.jsoup.Jsoup
import java.net.URL
import java.util.concurrent.TimeUnit

class DocumentedProblemsPagesFetcher {

  private companion object {
    private val subPagePathRegex = Regex("(api_changes_list_20..)\\.md")
    private const val MAIN_SOURCE_PAGE_URL = "https://jb.gg/ij-api-changes-raw"
    private const val MAIN_WEB_PAGE_URL = "https://plugins.jetbrains.com/docs/intellij/api-changes-list.html"
    private const val MAIN_EDIT_PAGE_URL = "https://jb.gg/ij-api-changes-edit"
  }

  fun fetchPages(url: String? = null): List<DocumentedProblemsPage> {
    val (mainUrl, mainPageBody) = fetchPageBody(url?.takeUnless(String::isNullOrBlank) ?: MAIN_SOURCE_PAGE_URL)
    val (mainWebUrl, _) = runCatching { fetchPageBody(MAIN_WEB_PAGE_URL) }.getOrNull() ?: MAIN_WEB_PAGE_URL to ""
    val (editUrl, _) = runCatching { fetchPageBody(MAIN_EDIT_PAGE_URL) }.getOrNull()
      ?: fetchPageBody("https://github.com/JetBrains/intellij-sdk-docs/edit/main/reference_guide/api_changes_list.md")
    val subPagesPaths = subPagePathRegex.findAll(mainPageBody).map { it.groups[1]!!.value }.toList()
    return subPagesPaths.map { path ->
      val sourcePageUrl = mainUrl.substringBeforeLast("/") + "/" + path + ".md"
      val webPageUrl = mainWebUrl.substringBeforeLast("/") + "/" + path + ".html"
      val editPageUrl = editUrl.substringBeforeLast("/") + "/" + path + ".md"
      val (_, pageBody) = fetchPageBody(sourcePageUrl)
      DocumentedProblemsPage(URL(webPageUrl), URL(sourcePageUrl), URL(editPageUrl), pageBody)
    }
  }

  /**
   * Returns resolved URL and the content of the page.
   */
  private fun fetchPageBody(pageUrl: String): Pair<String, String> {
    val url = "$pageUrl?flush_cache=true"
    return try {
      val response = Jsoup
        .connect(url)
        .timeout(TimeUnit.MINUTES.toMillis(5).toInt())
        .method(Connection.Method.GET)
        .execute()
      response.url().toExternalForm() to response.body()
    } catch (e: Exception) {
      throw RuntimeException("Unable to fetch body of page $url", e)
    }
  }
}
