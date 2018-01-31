package org.jetbrains.plugins.verifier.service.service.features

import com.google.gson.Gson
import com.jetbrains.pluginverifier.misc.makeOkHttpClient
import com.jetbrains.pluginverifier.network.createJsonRequestBody
import com.jetbrains.pluginverifier.network.createStringRequestBody
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.repository.PublicPluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import okhttp3.HttpUrl
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.jetbrains.plugins.verifier.service.setting.AuthorizationData
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

interface FeaturesRetrofitConnector {

  @Multipart
  @POST("/feature/getUpdatesToExtractFeatures")
  fun getUpdatesToExtractFeatures(@Part("userName") userName: RequestBody,
                                  @Part("password") password: RequestBody): Call<List<Int>>

  @Multipart
  @POST("/feature/receiveExtractedFeatures")
  fun sendExtractedFeatures(@Part("extractedFeatures") extractedFeatures: RequestBody,
                            @Part("userName") userName: RequestBody,
                            @Part("password") password: RequestBody): Call<ResponseBody>

}

class DefaultFeatureServiceProtocol(authorizationData: AuthorizationData,
                                    private val pluginRepository: PublicPluginRepository) : FeatureServiceProtocol {

  private val userNameRequestBody = createStringRequestBody(authorizationData.pluginRepositoryUserName)

  private val passwordRequestBody = createStringRequestBody(authorizationData.pluginRepositoryPassword)

  private val retrofitConnector: FeaturesRetrofitConnector by lazy {
    Retrofit.Builder()
        .baseUrl(HttpUrl.get(pluginRepository.repositoryURL))
        .addConverterFactory(GsonConverterFactory.create(Gson()))
        .client(makeOkHttpClient(false, 5, TimeUnit.MINUTES))
        .build()
        .create(FeaturesRetrofitConnector::class.java)
  }

  override fun getUpdatesToExtract(): List<UpdateInfo> =
      retrofitConnector
          .getUpdatesToExtractFeatures(userNameRequestBody, passwordRequestBody)
          .executeSuccessfully().body()
          .sortedDescending()
          .mapNotNull { pluginRepository.getPluginInfoById(it) }


  override fun sendExtractedFeatures(extractFeaturesResult: ExtractFeaturesTask.Result) {
    retrofitConnector
        .sendExtractedFeatures(
            createJsonRequestBody(extractFeaturesResult.prepareFeaturesResponse()),
            userNameRequestBody,
            passwordRequestBody
        ).executeSuccessfully()
  }
}
