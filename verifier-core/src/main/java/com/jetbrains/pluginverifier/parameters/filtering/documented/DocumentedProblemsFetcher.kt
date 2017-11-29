package com.jetbrains.pluginverifier.parameters.filtering.documented

import org.jsoup.Connection
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Patrikeev
 */
class DocumentedProblemsFetcher {

  fun fetchPage(documentedPageUrl: String): String =
      Jsoup
          .connect(documentedPageUrl)
          .timeout(TimeUnit.MINUTES.toMillis(5).toInt())
          .method(Connection.Method.GET)
          .execute()
          .body()
}