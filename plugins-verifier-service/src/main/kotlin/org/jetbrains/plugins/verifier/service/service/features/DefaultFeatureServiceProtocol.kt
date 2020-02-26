package org.jetbrains.plugins.verifier.service.service.features

import com.google.gson.Gson
import com.jetbrains.pluginverifier.misc.createOkHttpClient
import com.jetbrains.pluginverifier.network.createJsonRequestBody
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

private interface FeaturesRetrofitConnector {

  @GET("/api/updates/features-not-extracted")
  fun getUpdatesToExtractFeatures(@Header("Authorization") authorization: String): Call<List<UpdateAndPluginPair>>

  @POST("/api/features/receive-extracted-features")
  fun sendExtractedFeatures(
    @Header("Authorization") authorization: String,
    @Body extractedFeatures: RequestBody
  ): Call<ResponseBody>

}

private data class UpdateAndPluginPair(val updateId: Int, val pluginId: Int)

class DefaultFeatureServiceProtocol(
  token: String,
  private val pluginRepository: MarketplaceRepository
) : FeatureServiceProtocol {

  private val authorizationToken = "Bearer $token"

  private val retrofitConnector by lazy {
    Retrofit.Builder()
      .baseUrl(pluginRepository.repositoryURL.toHttpUrlOrNull()!!)
      .addConverterFactory(GsonConverterFactory.create(Gson()))
      .client(createOkHttpClient(false, 5, TimeUnit.MINUTES))
      .build()
      .create(FeaturesRetrofitConnector::class.java)
  }

  override fun getUpdatesToExtract(): List<UpdateInfo> {
    val pluginAndUpdatePairs = retrofitConnector
      .getUpdatesToExtractFeatures(authorizationToken)
      .executeSuccessfully().body()!!
    val pluginIdAndUpdateIds = pluginAndUpdatePairs.map { it.pluginId to it.updateId }
    return pluginRepository.getPluginInfosForManyIds(pluginIdAndUpdateIds).values.toList()
  }


  override fun sendExtractedFeatures(extractFeaturesResult: ExtractFeaturesTask.Result) {
    retrofitConnector
      .sendExtractedFeatures(
        authorizationToken,
        createJsonRequestBody(extractFeaturesResult.prepareFeaturesResponse())
      ).executeSuccessfully()
  }
}
