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
    const val DEFAULT_REPOSITORY = "JetBrains/intellij-sdk-docs"
    const val DEFAULT_BRANCH = "master"
  }

  fun fetchPages(repository: String? = null, branch: String? = null): List<DocumentedProblemsPage> {
    val repositoryName = repository.orEmpty().ifEmpty { DEFAULT_REPOSITORY }
    val branchName = branch.orEmpty().ifEmpty { DEFAULT_BRANCH }
    val mainSourcePageUrl = "https://raw.githubusercontent.com/$repositoryName/$branchName/reference_guide/api_changes_list.md"
    val mainWebPageUrl = "https://www.jetbrains.org/intellij/sdk/docs/reference_guide/api_changes_list.html"
    val mainEditPageUrl = "https://github.com/$repositoryName/edit/$branchName/reference_guide/api_changes_list.md"

    val mainPageBody = fetchPageBody(mainSourcePageUrl)
    val subPagesPaths = subPagePathRegex.findAll(mainPageBody).map { it.groups[1]!!.value }.toList()
    return subPagesPaths.map { path ->
      val sourcePageUrl = mainSourcePageUrl.substringBeforeLast("/") + "/" + path + ".md"
      val webPageUrl = mainWebPageUrl.substringBeforeLast("/") + "/" + path + ".html"
      val editPageUrl = mainEditPageUrl.substringBeforeLast("/") + "/" + path + ".md"
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
