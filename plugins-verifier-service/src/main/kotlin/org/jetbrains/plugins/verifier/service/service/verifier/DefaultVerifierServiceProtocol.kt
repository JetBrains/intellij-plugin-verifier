package org.jetbrains.plugins.verifier.service.service.verifier

import com.google.gson.Gson
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.makeOkHttpClient
import com.jetbrains.pluginverifier.network.createByteArrayRequestBody
import com.jetbrains.pluginverifier.network.createStringRequestBody
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.results.VerificationResult
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

interface VerifierRetrofitConnector {

  @POST("/verification/getUpdatesToCheck")
  @Multipart
  fun getUpdatesToCheck(@Part("availableIde") availableIde: RequestBody,
                        @Part("userName") userName: RequestBody,
                        @Part("password") password: RequestBody): Call<List<Int>>

  @POST("/verification/receiveUpdateCheckResult")
  @Multipart
  fun sendUpdateCheckResult(@Part("verificationResult") verificationResult: RequestBody,
                            @Part("userName") userName: RequestBody,
                            @Part("password") password: RequestBody): Call<ResponseBody>

}

class DefaultVerifierServiceProtocol(authorizationData: AuthorizationData,
                                     private val pluginRepository: PluginRepository) : VerifierServiceProtocol {

  private val retrofitConnector: VerifierRetrofitConnector by lazy {
    Retrofit.Builder()
        .baseUrl(HttpUrl.get(pluginRepository.repositoryURL))
        .addConverterFactory(GsonConverterFactory.create(Gson()))
        .client(makeOkHttpClient(false, 5, TimeUnit.MINUTES))
        .build()
        .create(VerifierRetrofitConnector::class.java)
  }

  private val userNameRequestBody = createStringRequestBody(authorizationData.pluginRepositoryUserName)

  private val passwordRequestBody = createStringRequestBody(authorizationData.pluginRepositoryPassword)

  override fun requestUpdatesToCheck(availableIde: IdeVersion): List<UpdateInfo> =
      retrofitConnector.getUpdatesToCheck(
          createStringRequestBody(availableIde.asString()),
          userNameRequestBody,
          passwordRequestBody
      )
          .executeSuccessfully().body()
          .sortedDescending()
          .mapNotNull { pluginRepository.getPluginInfoById(it) }

  override fun sendVerificationResult(verificationResult: VerificationResult) {
    retrofitConnector.sendUpdateCheckResult(
        createByteArrayRequestBody(verificationResult.prepareVerificationResponse().toByteArray()),
        userNameRequestBody,
        passwordRequestBody
    ).executeSuccessfully()
  }

}
