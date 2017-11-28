package com.jetbrains.pluginverifier.repository.plugins

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.jetbrains.pluginverifier.misc.makeOkHttpClient
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.repository.UpdateInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit

/**
 * Cache that provides mapping from update id, which is a unique identifier of
 * a plugin's build stored in the Plugin Repository, to corresponding [UpdateInfo].
 */
class UpdateInfoCache(repositoryUrl: String) {

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(UpdateInfoCache::class.java)
  }

  private val cache: ConcurrentMap<Int, UpdateInfo> = ConcurrentHashMap()

  private val api: GetUpdateInfoApi = Retrofit.Builder()
      .baseUrl(repositoryUrl)
      .addConverterFactory(GsonConverterFactory.create(Gson()))
      .client(makeOkHttpClient(false, 5, TimeUnit.MINUTES))
      .build()
      .create(GetUpdateInfoApi::class.java)

  fun addUpdateInfo(updateInfo: UpdateInfo) {
    cache.putIfAbsent(updateInfo.updateId, updateInfo)
  }

  private fun addUpdateInfo(updateInfoGson: UpdateInfoGson) {
    cache.putIfAbsent(updateInfoGson.updateId, updateInfoGson.toUpdateInfo())
  }

  private fun UpdateInfoGson.toUpdateInfo() = UpdateInfo(
      pluginId,
      pluginName,
      version,
      updateId,
      vendor
  )

  fun getUpdateInfo(updateId: Int): UpdateInfo? {
    val updateInfo = cache[updateId]
    if (updateInfo != null) {
      return updateInfo
    }
    updateBatch(updateId)
    return cache[updateId]
  }

  private fun updateBatch(updateId: Int) {
    try {
      val batchRequestSize = 1000
      val updates = api.getUpdateInfosForIds(updateId, updateId + batchRequestSize).executeSuccessfully().body()
      updates.forEach { addUpdateInfo(it) }
    } catch (e: Exception) {
      LOG.error("Unable to get UpdateInfo for Update #$updateId", e)
      updateSingle(updateId)
    }
  }

  private fun updateSingle(updateId: Int) {
    try {
      val info = api.getUpdateInfo(updateId).executeSuccessfully().body()
      addUpdateInfo(info)
    } catch (e: Exception) {
      LOG.error("Unable to get UpdateInfo #$updateId", e)
    }
  }
}

/**
 * Gson mirror class for [UpdateInfo] used to avoid possible field name conflicts.
 */
private data class UpdateInfoGson(@SerializedName("pluginId") val pluginId: String,
                                  @SerializedName("pluginName") val pluginName: String,
                                  @SerializedName("pluginVersion") val version: String,
                                  @SerializedName("updateId") val updateId: Int,
                                  @SerializedName("vendor") val vendor: String?)

private interface GetUpdateInfoApi {

  @POST("/manager/getUpdateInfosForIdsBetween")
  fun getUpdateInfosForIds(@Query("startId") startId: Int, @Query("endId") endId: Int): Call<List<UpdateInfoGson>>

  @POST("/manager/getUpdateInfoById")
  fun getUpdateInfo(@Query("updateId") updateId: Int): Call<UpdateInfoGson>

}
