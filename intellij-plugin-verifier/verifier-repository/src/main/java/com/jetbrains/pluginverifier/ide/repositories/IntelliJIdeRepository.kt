/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.ide.repositories

import com.google.common.base.Suppliers
import com.google.gson.annotations.SerializedName
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.pluginverifier.ide.AvailableIde
import com.jetbrains.pluginverifier.ide.IntelliJRepositoryIndexParser
import com.jetbrains.pluginverifier.misc.createOkHttpClient
import com.jetbrains.pluginverifier.network.executeSuccessfully
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Provides index of IDE builds available for downloading,
 * which is fetched from IntelliJ Artifacts Repositories.
 * - [releases](https://www.jetbrains.com/intellij-repository/releases)
 * - [snapshots](https://www.jetbrains.com/intellij-repository/snapshots)
 * - [nightly](https://www.jetbrains.com/intellij-repository/nightly) - available only with JetBrains VPN.
 */
class IntelliJIdeRepository(private val channel: Channel) : IdeRepository {

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
    Retrofit.Builder()
      .baseUrl("https://unused.com")
      .addConverterFactory(GsonConverterFactory.create())
      .client(createOkHttpClient(false, 5, TimeUnit.MINUTES))
      .build()
      .create(RepositoryIndexConnector::class.java)
  }

  private val indexCache = Suppliers.memoizeWithExpiration<List<AvailableIde>>(this::updateIndex, 1, TimeUnit.MINUTES)

  private fun updateIndex(): List<AvailableIde> {
    val index = repositoryIndexConnector.getIndex(channel.getIndexUrl())
    val artifacts = try {
      index.executeSuccessfully().body()!!.artifacts
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      if (channel == Channel.NIGHTLY) {
        throw IOException("Failed to fetch index from nightly channel. This channel is only accessible with JetBrains VPN connection", e)
      }
      throw e
    }
    return IntelliJRepositoryIndexParser().parseArtifacts(artifacts, channel)
  }

  private fun Channel.getIndexUrl() = "$repositoryUrl/index.json"

  override fun fetchIndex(): List<AvailableIde> = indexCache.get()

  override fun toString() = "IntelliJ Artifacts Repository (channel = ${channel.name.toLowerCase()})"

}

internal data class ArtifactsJson(
  @SerializedName("artifacts")
  val artifacts: List<ArtifactJson>
)

internal data class ArtifactJson(
  @SerializedName("groupId")
  val groupId: String,

  @SerializedName("artifactId")
  val artifactId: String,

  @SerializedName("version")
  val version: String,

  @SerializedName("packaging")
  val packaging: String,

  @SerializedName("content")
  val content: String?,

  @SerializedName("lastModifiedUnixTimeMs")
  val lastModifiedUnixTimeMs: Long
)

private interface RepositoryIndexConnector {

  @GET
  fun getIndex(@Url url: String): Call<ArtifactsJson>

}
