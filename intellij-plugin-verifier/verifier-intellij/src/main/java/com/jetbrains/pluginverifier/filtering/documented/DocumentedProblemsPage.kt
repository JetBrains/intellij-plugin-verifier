package com.jetbrains.pluginverifier.filtering.documented

import java.net.URL

data class DocumentedProblemsPage(
    val webPageUrl: URL,
    val sourcePageUrl: URL,
    val editPageUrl: URL,
    val pageBody: String
)