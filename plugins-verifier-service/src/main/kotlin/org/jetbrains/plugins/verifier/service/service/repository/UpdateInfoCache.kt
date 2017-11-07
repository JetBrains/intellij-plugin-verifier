package org.jetbrains.plugins.verifier.service.service.repository

import com.google.gson.Gson
import com.jetbrains.pluginverifier.misc.makeOkHttpClient
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.repository.UpdateInfo
import org.jetbrains.plugins.verifier.service.setting.Settings
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
 * @author Sergey Patrikeev
 */
object UpdateInfoCache {

  private val LOG: Logger = LoggerFactory.getLogger(UpdateInfoCache.javaClass)

  private val cache: ConcurrentMap<Int, UpdateInfo> = ConcurrentHashMap()

  private val api: GetUpdateInfoApi = Retrofit.Builder()
      .baseUrl(Settings.DOWNLOAD_PLUGINS_REPOSITORY_URL.get())
      .addConverterFactory(GsonConverterFactory.create(Gson()))
      .client(makeOkHttpClient(false, 5, TimeUnit.MINUTES))
      .build()
      .create(GetUpdateInfoApi::class.java)

  fun update(updateInfo: UpdateInfo) {
    cache.putIfAbsent(updateInfo.updateId, updateInfo)
  }

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
      updates.forEach { update(it) }
    } catch (e: Exception) {
      LOG.error("Unable to get UpdateInfo for Update #$updateId", e)
      updateSingle(updateId)
    }
  }

  private fun updateSingle(updateId: Int) {
    try {
      val info = api.getUpdateInfo(updateId).executeSuccessfully().body()
      update(info)
    } catch (e: Exception) {
      LOG.error("Unable to get UpdateInfo #$updateId", e)
    }
  }
}

private interface GetUpdateInfoApi {

  @POST("/manager/getUpdateInfosForIdsBetween")
  fun getUpdateInfosForIds(@Query("startId") startId: Int, @Query("endId") endId: Int): Call<List<UpdateInfo>>

  @POST("/manager/getUpdateInfoById")
  fun getUpdateInfo(@Query("updateId") updateId: Int): Call<UpdateInfo>

}
