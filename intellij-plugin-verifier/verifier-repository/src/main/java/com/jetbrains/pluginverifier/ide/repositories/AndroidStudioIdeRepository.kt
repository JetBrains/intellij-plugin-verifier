/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.ide.repositories

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.plugin.structure.ide.IntelliJPlatformProduct
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.AvailableIde
import com.jetbrains.pluginverifier.misc.RestApiFailed
import com.jetbrains.pluginverifier.misc.RestApiOk
import com.jetbrains.pluginverifier.misc.RestApis
import com.jetbrains.pluginverifier.repository.cache.memoize
import java.net.URL
import java.time.LocalDate

private const val FEED_URL = "https://download.jetbrains.com/toolbox/feeds"

class AndroidStudioIdeRepository(private val feedUrl: String = FEED_URL) : IdeRepository {

  private val feedConnector by lazy {
    AndroidStudioFeedConnector(feedUrl)
  }

  private val indexCache = memoize {
    updateIndex()
  }

  private fun updateIndex(): List<AvailableIde> {
    val feedEntries = feedConnector.getFeed()
    return feedEntries
            .filter { it.packageInfo.os == "linux" }
            .map {
              val ideVersion = IdeVersion.createIdeVersion(it.build).setProductCodeIfAbsent("AI")
              val uploadDate = getApproximateUploadDate(ideVersion)
              AvailableIde(
                      ideVersion,
                      it.version,
                      it.packageInfo.url,
                      uploadDate,
                      IntelliJPlatformProduct.ANDROID_STUDIO
              )
            }
  }

  @Throws(InterruptedException::class)
  override fun fetchIndex(): List<AvailableIde> = indexCache.get()

  /**
   * Android Studio feed does not provide info on when IDE builds were uploaded.
   * Let's approximate it based on build number.
   */
  private fun getApproximateUploadDate(ideVersion: IdeVersion): LocalDate {
    //191 (2019.1), 192, 193, 201, 202, 203, 211, 212, 213 ...
    val branch = ideVersion.baselineVersion
    val year = 2000 + branch / 10
    val month = (12 / 3) * (branch % 10) - 1
    return LocalDate.of(year, month, 1)
  }
}

private class AndroidStudioFeedConnector(private val feedBaseUri: String) {
  private val restApi = RestApis()

  fun getFeed(): List<FeedEntry> {
    val uri = "$feedBaseUri/v1/android-studio.feed.xz.signed"
    return when (val apiResult = restApi.getSigned(uri, Feed::class.java)) {
      is RestApiOk<Feed> -> apiResult.payload.entries
      is RestApiFailed<*> -> emptyList()
    }
  }
}

private data class Feed(@JsonProperty("entries") val entries: List<FeedEntry>)

private data class FeedEntry(
        @JsonProperty("build")
        val build: String,

        @JsonProperty("version")
        val version: String,

        @JsonProperty("package")
        val packageInfo: PackageInfo
)

private data class PackageInfo(
        @JsonProperty("url")
        val url: URL,

        @JsonProperty("os")
        val os: String
)
