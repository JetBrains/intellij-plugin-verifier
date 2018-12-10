package com.jetbrains.pluginverifier.ide

import com.google.common.base.Suppliers
import com.google.gson.annotations.SerializedName
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.createOkHttpClient
import com.jetbrains.pluginverifier.network.executeSuccessfully
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

/**
 * Provides index of IDE builds available for downloading,
 * which is fetched from IntelliJ
 * [release](https://www.jetbrains.com/intellij-repository/release/) or
 * [snapshots](https://www.jetbrains.com/intellij-repository/snapshots)
 * repository, depending on
 */
class IntelliJIdeRepository(private val snapshotsChannel: Boolean) : IdeRepository {

  companion object {
    const val INTELLIJ_REPOSITORY_URL = "https://www.jetbrains.com/intellij-repository/"

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
        Triple("clion", "com.jetbrains.intellij.clion", "CL")
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
        .baseUrl(INTELLIJ_REPOSITORY_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(createOkHttpClient(false, 5, TimeUnit.MINUTES))
        .build()
        .create(RepositoryIndexConnector::class.java)
  }

  private val indexCache = Suppliers.memoizeWithExpiration<List<AvailableIde>>(this::updateIndex, 1, TimeUnit.MINUTES)

  private fun updateIndex(): List<AvailableIde> {
    val index = if (snapshotsChannel) {
      repositoryIndexConnector.getSnapshotsIndex()
    } else {
      repositoryIndexConnector.getReleasesIndex()
    }
    val artifacts = index
        .executeSuccessfully().body()
        .artifacts
    return IntelliJRepositoryIndexParser().parseArtifacts(artifacts, snapshotsChannel)
  }

  override fun fetchIndex(): List<AvailableIde> = indexCache.get()

  override fun fetchAvailableIde(ideVersion: IdeVersion): AvailableIde? {
    val fullIdeVersion = ideVersion.setProductCodeIfAbsent("IU")
    return fetchIndex().find { it.version == fullIdeVersion }
  }

  override fun toString() = "IDE Repository on $INTELLIJ_REPOSITORY_URL"

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
  fun getReleasesIndex(): Call<ArtifactsJson>

  @GET("snapshots/index.json")
  fun getSnapshotsIndex(): Call<ArtifactsJson>
}
