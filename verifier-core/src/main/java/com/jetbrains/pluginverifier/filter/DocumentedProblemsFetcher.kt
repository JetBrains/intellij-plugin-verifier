package com.jetbrains.pluginverifier.filter

import org.jsoup.Connection
import org.jsoup.Jsoup

/**
 * @author Sergey Patrikeev
 */
class DocumentedProblemsFetcher {

  fun fetchPage(documentedPageUrl: String): String =
      Jsoup
          .connect(documentedPageUrl)
          .timeout(3000)
          .method(Connection.Method.GET)
          .execute()
          .body()
}