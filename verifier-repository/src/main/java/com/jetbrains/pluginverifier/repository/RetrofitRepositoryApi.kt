package com.jetbrains.pluginverifier.repository

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

internal interface RetrofitRepositoryApi {

  @GET("/manager/getUpdateInfoById")
  fun getUpdateInfoById(@Query("updateId") updateId: Int): Call<UpdateInfoResponse>

  @GET("/manager/allCompatibleUpdates")
  fun getLastCompatibleUpdates(@Query("build") build: String): Call<List<UpdateInfoResponse>>

  @GET("/plugin/updates")
  fun getUpdates(@Query("xmlId") xmlId: String): Call<UpdatesResponse>

  @GET("/manager/originalCompatibleUpdatesByPluginIds")
  fun getOriginalCompatibleUpdatesByPluginIds(@Query("build") build: String, @Query("pluginIds") pluginId: String): Call<List<UpdateInfoResponse>>

  @GET("/plugin/download/?noStatistic=true")
  @Streaming
  fun downloadFile(@Query("updateId") updateId: Int): Call<ResponseBody>
}

internal data class UpdateInfoResponse(@SerializedName("pluginId") val pluginId: String,
                                       @SerializedName("pluginName") val pluginName: String,
                                       @SerializedName("version", alternate = arrayOf("pluginVersion")) val version: String,
                                       @SerializedName("updateId") val updateId: Int,
                                       @SerializedName("vendor") val vendor: String?)

internal fun UpdateInfoResponse.toUpdateInfo(): UpdateInfo = UpdateInfo(pluginId, pluginName, version, updateId, vendor)

internal data class UpdatesResponse(@SerializedName("pluginXmlId") val pluginXmlId: String,
                                    @SerializedName("pluginName") val pluginName: String,
                                    @SerializedName("vendor") val vendor: String,
                                    @SerializedName("updates") val updates: List<Update>) {
  data class Update(val id: Int, val updateVersion: String, val since: String?, val until: String?)
}