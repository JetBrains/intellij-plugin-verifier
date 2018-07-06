package org.jetbrains.plugins.verifier.service.service.verifier

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.createOkHttpClient
import com.jetbrains.pluginverifier.network.createByteArrayRequestBody
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import okhttp3.HttpUrl
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.jetbrains.plugins.verifier.service.setting.AuthorizationData
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

class DefaultVerifierServiceProtocol(
    authorizationData: AuthorizationData,
    private val pluginRepository: MarketplaceRepository
) : VerifierServiceProtocol {

  private val retrofitConnector: VerifierRetrofitConnector by lazy {
    Retrofit.Builder()
        .baseUrl(HttpUrl.get(pluginRepository.repositoryURL))
        .addConverterFactory(GsonConverterFactory.create(Gson()))
        .client(createOkHttpClient(false, 5, TimeUnit.MINUTES))
        .build()
        .create(VerifierRetrofitConnector::class.java)
  }

  private val authorizationToken = "Bearer ${authorizationData.pluginRepositoryAuthorizationToken}"

  override fun requestScheduledVerifications(): List<ScheduledVerification> =
      retrofitConnector
          .getScheduledVerifications(authorizationToken)
          .executeSuccessfully().body()
          .mapNotNull {
            val updateInfo = pluginRepository.getPluginInfoById(it.updateId)
            val ideVersion = IdeVersion.createIdeVersionIfValid(it.ideVersion)
            val manually = it.manually
            if (updateInfo != null && ideVersion != null) {
              ScheduledVerification(updateInfo, ideVersion, manually)
            } else {
              null
            }
          }

  override fun sendVerificationResult(verificationResult: VerificationResult, updateInfo: UpdateInfo) {
    retrofitConnector.sendVerificationResult(
        createByteArrayRequestBody(verificationResult.prepareVerificationResponse(updateInfo).toByteArray()),
        authorizationToken
    ).executeSuccessfully()
  }

}

private interface VerifierRetrofitConnector {

  @GET("/verification/getScheduledVerifications")
  fun getScheduledVerifications(@Header("Authorization") authorization: String): Call<List<ScheduledVerificationJson>>

  @POST("/verification/receiveVerificationResult")
  @Multipart
  fun sendVerificationResult(
      @Part("verificationResult") verificationResult: RequestBody,
      @Header("Authorization") authorization: String
  ): Call<ResponseBody>

}

private data class ScheduledVerificationJson(
    @SerializedName("updateId") val updateId: Int,
    @SerializedName("ideVersion") val ideVersion: String,
    @SerializedName("manually") val manually: Boolean
)