package com.jetbrains.pluginverifier.repository

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

internal interface RetrofitRepositoryApi {

  @GET("/manager/getUpdateInfoById")
  fun getUpdateInfoById(@Query("updateId") updateId: Int): Call<UpdateInfo>

  @GET("/manager/allCompatibleUpdates")
  fun getLastCompatibleUpdates(@Query("build") build: String): Call<List<UpdateInfo>>

  @GET("/plugin/updates")
  fun getUpdates(@Query("xmlId") xmlId: String): Call<UpdatesResponse>

  @GET("/manager/originalCompatibleUpdatesByPluginIds")
  fun getOriginalCompatibleUpdatesByPluginIds(@Query("build") build: String, @Query("pluginIds") pluginId: String): Call<List<UpdateInfo>>

  @GET("/plugin/download/?noStatistic=true")
  @Streaming
  fun downloadFile(@Query("updateId") updateId: Int): Call<ResponseBody>
}

internal data class UpdatesResponse(val pluginXmlId: String,
                                    val pluginName: String,
                                    val vendor: String,
                                    val updates: List<Update>) {
  data class Update(val id: Int, val updateVersion: String, val since: String?, val until: String?)
}