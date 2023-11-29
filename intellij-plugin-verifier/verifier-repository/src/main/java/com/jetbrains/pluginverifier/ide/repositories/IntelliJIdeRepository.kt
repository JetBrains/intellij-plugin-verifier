/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.ide.repositories

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.pluginverifier.ide.AvailableIde
import com.jetbrains.pluginverifier.ide.IntelliJRepositoryIndexParser
import com.jetbrains.pluginverifier.misc.RestApiFailed
import com.jetbrains.pluginverifier.misc.RestApiOk
import com.jetbrains.pluginverifier.misc.RestApis
import com.jetbrains.pluginverifier.repository.cache.memoize
import java.io.IOException
import java.util.*

/**
 * Provides index of IDE builds available for downloading,
 * which is fetched from IntelliJ Artifacts Repositories.
 * - [releases](https://www.jetbrains.com/intellij-repository/releases)
 * - [snapshots](https://www.jetbrains.com/intellij-repository/snapshots)
 * - [nightly](https://www.jetbrains.com/intellij-repository/nightly) - available only with JetBrains VPN.
 */
open class IntelliJIdeRepository(private val channel: Channel) : IdeRepository {
  enum class Channel(val repositoryUrl: String) {
    RELEASE("https://cache-redirector.jetbrains.com/intellij-repository/releases"),
    SNAPSHOTS("https://cache-redirector.jetbrains.com/intellij-repository/snapshots"),
    NIGHTLY("https://www.jetbrains.com/intellij-repository/nightly")
  }

  companion object {

    /**
     * Maps artifact IDs, group IDs and corresponding product codes.
     *
     * If a new IDE is published to the repository, it should be registered here.
     */
    private val artifactIdGroupIdAndProductCode = listOf(
      Triple("ideaIC", "com.jetbrains.intellij.idea", "IC"),
      Triple("ideaIU", "com.jetbrains.intellij.idea", "IU"),
      Triple("riderRD", "com.jetbrains.intellij.rider", "RD"),
      Triple("mps", "com.jetbrains.mps", "MPS"),
      Triple("clion", "com.jetbrains.intellij.clion", "CL"),
      Triple("goland", "com.jetbrains.intellij.goland", "GO")
    )

    fun getArtifactIdByProductCode(productCode: String): String? =
      artifactIdGroupIdAndProductCode.find { it.third == productCode }?.first

    fun getGroupIdByProductCode(productCode: String): String? =
      artifactIdGroupIdAndProductCode.find { it.third == productCode }?.second

    fun getProductCodeByArtifactId(artifactId: String): String? =
      artifactIdGroupIdAndProductCode.find { it.first == artifactId }?.third
  }

  private val repositoryIndexConnector by lazy {
    RepositoryIndexConnector(indexBaseUrl)
  }

  private val indexCache = memoize(expirationInMinutes = 1) { updateIndex() }

  private fun updateIndex(): List<AvailableIde> {
    try {
      val artifacts = repositoryIndexConnector.getIndex()
      return IntelliJRepositoryIndexParser().parseArtifacts(artifacts, channel)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      if (channel == Channel.NIGHTLY) {
        throw IOException("Failed to fetch index from nightly channel. This channel is only accessible with JetBrains VPN connection", e)
      }
      throw e
    }
  }

  protected open val indexBaseUrl: String
    get() = channel.repositoryUrl

  override fun fetchIndex(): List<AvailableIde> = indexCache.get()

  override fun toString() = "IntelliJ Artifacts Repository (channel = ${channel.name.lowercase(Locale.getDefault())})"

}

internal data class ArtifactsJson(
  @JsonProperty("artifacts")
  val artifacts: List<ArtifactJson>
)

internal data class ArtifactJson(
  @JsonProperty("groupId")
  val groupId: String,

  @JsonProperty("artifactId")
  val artifactId: String,

  @JsonProperty("version")
  val version: String,

  @JsonProperty("packaging")
  val packaging: String,

  @JsonProperty("content")
  val content: String?,

  @JsonProperty("lastModifiedUnixTimeMs")
  val lastModifiedUnixTimeMs: Long
)

private class RepositoryIndexConnector(private val indexBaseUri: String) {
  private val restApi = RestApis()

  fun getIndex(): List<ArtifactJson> {
    val uri = "$indexBaseUri/index.json"

    return when (val apiResult = restApi.get(uri, ArtifactsJson::class.java)) {
      is RestApiOk<ArtifactsJson> -> apiResult.payload.artifacts
      is RestApiFailed<*> -> emptyList()
    }
  }
}
