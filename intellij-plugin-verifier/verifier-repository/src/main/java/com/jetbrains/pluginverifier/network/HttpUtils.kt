package com.jetbrains.pluginverifier.network

import java.net.URL

object HttpHeaders {
  const val CONTENT_DISPOSITION = "Content-Disposition"
  const val CONTENT_TYPE = "Content-Type"
  const val CONTENT_LENGTH = "Content-Length"
}

/**
 * `equals()` for URL that doesn't require internet connection in contrast to [URL.equals]
 */
fun URL.safeEquals(other: URL): Boolean = toExternalForm().trimEnd('/') == other.toExternalForm().trimEnd('/')

/**
 * `hashCode()` for URL that doesn't require internet connection in contrast to [URL.hashCode]
 */
fun URL.safeHashCode(): Int = toExternalForm().trim('/').hashCode()