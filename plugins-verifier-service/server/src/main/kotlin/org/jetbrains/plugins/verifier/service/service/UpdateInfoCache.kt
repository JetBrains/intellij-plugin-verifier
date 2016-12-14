package org.jetbrains.plugins.verifier.service.service

import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.persistence.GsonHolder
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.util.executeSuccessfully
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * @author Sergey Patrikeev
 */
object UpdateInfoCache {

  private val LOG: Logger = LoggerFactory.getLogger(UpdateInfoCache.javaClass)

  private val cache: ConcurrentMap<Int, UpdateInfo> = ConcurrentHashMap()

  private val api: GetUpdateInfoApi = Retrofit.Builder()
      .baseUrl(Settings.PLUGIN_REPOSITORY_URL.get())
      .addConverterFactory(GsonConverterFactory.create(GsonHolder.GSON))
      .client(makeClient(LOG.isDebugEnabled))
      .build()
      .create(GetUpdateInfoApi::class.java)

  fun getUpdateInfo(updateId: Int): UpdateInfo? {
    val updateInfo = cache[updateId]
    if (updateInfo != null) {
      return updateInfo
    }
    try {
      val info = api.getUpdateInfo(updateId).executeSuccessfully().body()
      cache.putIfAbsent(updateId, info)
      return info
    } catch (e: Exception) {
      LOG.error("Unable to get UpdateInfo #$updateId", e)
      return null
    }
    /*
    try {
      val batchRequestSize = 1000
      val updates = api.getUpdateInfosForIds(updateId, updateId + batchRequestSize).executeSuccessfully().body()
      updates.forEach {
        cache.putIfAbsent(it.updateId, it)
      }
      return cache[updateId]
    } catch (e: Exception) {
      LOG.error("Unable to get UpdateInfo for Update #$updateId", e)
      return null
    }
*/
  }
}

interface GetUpdateInfoApi {

  @POST("/manager/getUpdateInfosForIdsBetween")
  fun getUpdateInfosForIds(@Query("startId") startId: Int, @Query("endId") endId: Int): Call<List<UpdateInfo>>

  @POST("/manager/getUpdateInfoById")
  fun getUpdateInfo(@Query("updateId") updateId: Int): Call<UpdateInfo>

}
