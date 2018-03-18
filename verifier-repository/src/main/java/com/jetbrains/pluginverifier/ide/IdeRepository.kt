package com.jetbrains.pluginverifier.ide

import com.google.gson.annotations.SerializedName
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.makeOkHttpClient
import com.jetbrains.pluginverifier.network.executeSuccessfully
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

/**
 * Utility class that requests metadata from the [IDE repository](https://www.jetbrains.com/intellij-repository/releases)
 * on [available] [AvailableIde] IDEs.
 */
class IdeRepository {

  companion object {
    const val IDE_REPOSITORY_URL = "https://www.jetbrains.com/intellij-repository/"
  }

  private val repositoryIndexConnector by lazy {
    Retrofit.Builder()
        .baseUrl(IDE_REPOSITORY_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(makeOkHttpClient(false, 5, TimeUnit.MINUTES))
        .build()
        .create(RepositoryIndexConnector::class.java)
  }

  fun fetchIndex(snapshots: Boolean = false): List<AvailableIde> {
    val artifacts = if (snapshots) {
      repositoryIndexConnector.getSnapshotsIndex()
    } else {
      repositoryIndexConnector.getReleaseIndex()
    }.executeSuccessfully().body().artifacts
    return IdeRepositoryIndexParser(IDE_REPOSITORY_URL)
        .parseArtifacts(artifacts, snapshots)
  }

  fun fetchAvailableIdeDescriptor(ideVersion: IdeVersion, snapshots: Boolean): AvailableIde? {
    val fullIdeVersion = ideVersion.setProductCodeIfAbsent("IU")
    return fetchIndex(snapshots).find { it.version == fullIdeVersion }
  }

  override fun toString() = "IDE Repository on $IDE_REPOSITORY_URL"

}

fun IdeVersion.setProductCodeIfAbsent(productCode: String) =
    if (this.productCode.isEmpty())
      IdeVersion.createIdeVersion("$productCode-" + asStringWithoutProductCode())
    else {
      this
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
    val content: String?
)

private interface RepositoryIndexConnector {
  @GET("releases/index.json")
  fun getReleaseIndex(): Call<ArtifactsJson>

  @GET("snapshots/index.json")
  fun getSnapshotsIndex(): Call<ArtifactsJson>
}
