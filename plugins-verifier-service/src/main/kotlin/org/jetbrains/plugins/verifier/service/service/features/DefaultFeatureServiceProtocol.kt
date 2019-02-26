package org.jetbrains.plugins.verifier.service.service.features

import com.google.gson.Gson
import com.jetbrains.pluginverifier.misc.createOkHttpClient
import com.jetbrains.pluginverifier.network.createJsonRequestBody
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import okhttp3.HttpUrl
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.jetbrains.plugins.verifier.service.setting.AuthorizationData
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

private interface FeaturesRetrofitConnector {

  @GET("/feature/getUpdatesToExtractFeatures")
  fun getUpdatesToExtractFeatures(@Header("Authorization") authorization: String): Call<List<Int>>

  @POST("/feature/receiveExtractedFeatures")
  fun sendExtractedFeatures(
      @Header("Authorization") authorization: String,
      @Body extractedFeatures: RequestBody
  ): Call<ResponseBody>

}

class DefaultFeatureServiceProtocol(
    authorizationData: AuthorizationData,
    private val pluginRepository: MarketplaceRepository
) : FeatureServiceProtocol {

  private val authorizationToken = "Bearer ${authorizationData.pluginRepositoryAuthorizationToken}"

  private val retrofitConnector by lazy {
    Retrofit.Builder()
        .baseUrl(HttpUrl.get(pluginRepository.repositoryURL))
        .addConverterFactory(GsonConverterFactory.create(Gson()))
        .client(createOkHttpClient(false, 5, TimeUnit.MINUTES))
        .build()
        .create(FeaturesRetrofitConnector::class.java)
  }

  override fun getUpdatesToExtract(): List<UpdateInfo> =
      retrofitConnector
          .getUpdatesToExtractFeatures(authorizationToken)
          .executeSuccessfully().body()
          .sortedDescending()
          .mapNotNull { pluginRepository.getPluginInfoById(it) }


  override fun sendExtractedFeatures(extractFeaturesResult: ExtractFeaturesTask.Result) {
    retrofitConnector
        .sendExtractedFeatures(
            authorizationToken,
            createJsonRequestBody(extractFeaturesResult.prepareFeaturesResponse())
        ).executeSuccessfully()
  }
}
