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
    private val subPagePathRegex = Regex("(api_changes/api_changes_list_20..)\\.md")
    const val MAIN_SOURCE_PAGE_URL = "https://raw.githubusercontent.com/JetBrains/intellij-sdk-docs/master/reference_guide/api_changes_list.md"
    const val MAIN_WEB_PAGE_URL = "https://www.jetbrains.org/intellij/sdk/docs/reference_guide/api_changes_list.html"
    const val MAIN_EDIT_PAGE_URL = "https://github.com/JetBrains/intellij-sdk-docs/edit/master/reference_guide/api_changes_list.md"
  }

  fun fetchPages(): List<DocumentedProblemsPage> {
    val mainPageBody = fetchPageBody(MAIN_SOURCE_PAGE_URL)
    val subPagesPaths = subPagePathRegex.findAll(mainPageBody).map { it.groups[1]!!.value }.toList()
    return subPagesPaths.map { path ->
      val sourcePageUrl = MAIN_SOURCE_PAGE_URL.substringBeforeLast("/") + "/" + path + ".md"
      val webPageUrl = MAIN_WEB_PAGE_URL.substringBeforeLast("/") + "/" + path + ".html"
      val editPageUrl = MAIN_EDIT_PAGE_URL.substringBeforeLast("/") + "/" + path + ".md"
      val pageBody = fetchPageBody(sourcePageUrl)
      DocumentedProblemsPage(URL(webPageUrl), URL(sourcePageUrl), URL(editPageUrl), pageBody)
    }
  }

  private fun fetchPageBody(pageUrl: String) = try {
    Jsoup
      .connect(pageUrl)
      .timeout(TimeUnit.MINUTES.toMillis(5).toInt())
      .method(Connection.Method.GET)
      .execute()
      .body()
  } catch (e: Exception) {
    throw RuntimeException("Unable to fetch body of page $pageUrl", e)
  }

}