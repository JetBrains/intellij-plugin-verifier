package com.jetbrains.pluginverifier.repository.retrofit

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * The Retrofit connector used to communicate with https://plugins.jetbrains.com.
 */
internal interface PublicPluginRepositoryConnector {

  @GET("/manager/getUpdateInfoById")
  fun getUpdateInfoById(@Query("updateId") updateId: Int): Call<JsonUpdateInfo>

  @GET("/manager/allCompatibleUpdates")
  fun getAllCompatibleUpdates(@Query("build") build: String): Call<List<JsonUpdateInfo>>

  @GET("/plugin/updates")
  fun getPluginUpdates(@Query("xmlId") xmlId: String): Call<JsonUpdatesResponse>

  @POST("/manager/getUpdateInfosForIdsBetween")
  fun getUpdateInfosForIdsBetween(@Query("startId") startId: Int, @Query("endId") endId: Int): Call<List<JsonUpdateInfo>>

  /**
   * Returns all plugins with "until build" >= [build].
   * The [startUpdateId] is used to limit the response size,
   * and reduce the load on the Plugin Repository.
   */
  @GET("/manager/allUpdatesSince")
  fun getAllUpdateSinceAndUntil(@Query("build") build: String, @Query("updateId") startUpdateId: Int): Call<List<JsonUpdateSinceUntil>>
}

internal data class JsonUpdateInfo(@SerializedName("pluginId") val pluginId: String,
                                   @SerializedName("pluginName") val pluginName: String,
                                   @SerializedName("version", alternate = arrayOf("pluginVersion")) val version: String,
                                   @SerializedName("updateId") val updateId: Int,
                                   @SerializedName("vendor") val vendor: String,
                                   @SerializedName("since") val sinceString: String,
                                   @SerializedName("until") val untilString: String)

internal data class JsonUpdatesResponse(@SerializedName("pluginXmlId") val pluginId: String,
                                        @SerializedName("pluginName") val pluginName: String,
                                        @SerializedName("vendor") val vendor: String,
                                        @SerializedName("updates") val updates: List<JsonUpdate>) {

  data class JsonUpdate(
      @SerializedName("id")
      val updateId: Int,

      @SerializedName("updateVersion")
      val updateVersion: String,

      @SerializedName("since")
      val sinceBuild: String,

      @SerializedName("until")
      val untilBuild: String
  )
}

internal data class JsonUpdateSinceUntil(
    @SerializedName("updateId") val updateId: Int,
    @SerializedName("pluginId") val pluginDatabaseId: Int,
    @SerializedName("since") val sinceBuild: String,
    @SerializedName("until") val untilBuild: String
)
