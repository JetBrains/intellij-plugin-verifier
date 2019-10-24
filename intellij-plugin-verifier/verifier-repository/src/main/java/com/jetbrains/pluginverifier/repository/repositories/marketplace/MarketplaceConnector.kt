package com.jetbrains.pluginverifier.repository.repositories.marketplace

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * The Retrofit connector used to communicate with https://plugins.jetbrains.com.
 */
internal interface MarketplaceConnector {

  @GET("/manager/getUpdateInfoById")
  fun getUpdateInfoById(@Query("updateId") updateId: Int): Call<JsonUpdateInfo>

  @GET("/manager/allCompatibleUpdates")
  fun getAllCompatibleUpdates(@Query("build") build: String): Call<List<JsonUpdateInfo>>

  @GET("/plugin/updates")
  fun getPluginUpdates(@Query("xmlId") xmlId: String): Call<JsonUpdatesIdsHolder>

  @GET("/manager/getUpdateInfosForIdsBetween")
  fun getUpdateInfosForIdsBetween(@Query("startId") startId: Int, @Query("endId") endId: Int): Call<List<JsonUpdateInfo>>

}

internal data class JsonUpdateInfo(
  @SerializedName("pluginId") val pluginId: String,
  @SerializedName("pluginName") val pluginName: String,
  @SerializedName("version", alternate = arrayOf("pluginVersion")) val version: String,
  @SerializedName("updateId") val updateId: Int,
  @SerializedName("vendor") val vendor: String,
  @SerializedName("since") val sinceString: String,
  @SerializedName("until") val untilString: String,
  @SerializedName("sourceCodeUrl") val sourceCodeUrl: String?,
  @SerializedName("tags") val tags: List<String>?
)

internal data class JsonUpdatesIdsHolder(
  @SerializedName("updates")
  val updateIds: List<JsonUpdateIdHolder>
)

internal data class JsonUpdateIdHolder(
  @SerializedName("updateId", alternate = ["id"])
  val updateId: Int
)