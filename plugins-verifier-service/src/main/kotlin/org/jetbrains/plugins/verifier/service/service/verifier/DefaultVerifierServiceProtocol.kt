package org.jetbrains.plugins.verifier.service.service.verifier

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.createOkHttpClient
import com.jetbrains.pluginverifier.network.byteArrayMediaType
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.network.stringMediaType
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
    val verificationResponse = verificationResult.prepareVerificationResponse(updateInfo)
    retrofitConnector.uploadVerificationResultContent(
        authorizationToken,
        updateInfo.updateId,
        verificationResponse.ideVersion,
        RequestBody.create(byteArrayMediaType, verificationResponse.toByteArray())
    ).executeSuccessfully()

    retrofitConnector.sendVerificationResult(
        authorizationToken,
        updateInfo.updateId,
        RequestBody.create(stringMediaType, verificationResponse.ideVersion),
        RequestBody.create(stringMediaType, verificationResult.verificationVerdict),
        RequestBody.create(stringMediaType, verificationResponse.resultType.name)
    ).executeSuccessfully()
  }

}

private interface VerifierRetrofitConnector {

  @GET("/verification/getScheduledVerifications")
  fun getScheduledVerifications(@Header("Authorization") authorization: String): Call<List<ScheduledVerificationJson>>

  @PUT("/verification/uploadVerificationResultContent")
  fun uploadVerificationResultContent(
      @Header("Authorization") authorization: String,
      @Query("updateId") updateId: Int,
      @Query("ideVersion") ideVersion: String,
      @Body content: RequestBody
  ): Call<ResponseBody>

  @POST("/verification/receiveVerificationResult")
  @Multipart
  fun sendVerificationResult(
      @Header("Authorization") authorization: String,
      @Part("updateId") updateId: Int,
      @Part("ideVersion") ideVersion: RequestBody,
      @Part("verdict") verdict: RequestBody,
      @Part("resultType") resultType: RequestBody
  ): Call<ResponseBody>

}

private data class ScheduledVerificationJson(
    @SerializedName("updateId") val updateId: Int,
    @SerializedName("ideVersion") val ideVersion: String,
    @SerializedName("manually") val manually: Boolean
)