package org.jetbrains.plugins.verifier.service.service.verifier

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.ide.repositories.IdeRepository
import com.jetbrains.pluginverifier.misc.createOkHttpClient
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.network.jsonMediaType
import com.jetbrains.pluginverifier.network.stringMediaType
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import okhttp3.HttpUrl
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

class DefaultVerifierServiceProtocol(
    token: String,
    private val pluginRepository: MarketplaceRepository,
    private val ideRepository: IdeRepository
) : VerifierServiceProtocol {

  private val retrofitConnector: VerifierRetrofitConnector by lazy {
    Retrofit.Builder()
        .baseUrl(HttpUrl.get(pluginRepository.repositoryURL))
        .addConverterFactory(GsonConverterFactory.create(Gson()))
        .client(createOkHttpClient(false, 5, TimeUnit.MINUTES))
        .build()
        .create(VerifierRetrofitConnector::class.java)
  }

  private val authorizationToken = "Bearer $token"

  private val json = Gson()

  override fun requestScheduledVerifications(): List<ScheduledVerification> =
    retrofitConnector
      .getScheduledVerifications(authorizationToken)
      .executeSuccessfully().body()
      .mapNotNull { buildScheduledVerification(it) }

  private fun buildScheduledVerification(json: ScheduledVerificationJson): ScheduledVerification? {
    val ideVersion = IdeVersion.createIdeVersionIfValid(json.availableIde.buildNumber) ?: return null
    val availableIde = ideRepository.fetchAvailableIde(ideVersion) ?: return null
    val updateInfo = pluginRepository.getPluginInfoById(json.updateId) ?: return null
    return ScheduledVerification(updateInfo, availableIde, json.manually)
  }

  override fun sendVerificationResult(scheduledVerification: ScheduledVerification, verificationResult: PluginVerificationResult) {
    val verificationResultData = verificationResult.prepareResponse(scheduledVerification)
    retrofitConnector.uploadVerificationResultContent(
      authorizationToken,
      scheduledVerification.updateInfo.updateId,
      verificationResultData.ideVersion.ideVersion,
      RequestBody.create(jsonMediaType, json.toJson(verificationResultData))
    ).executeSuccessfully()

    retrofitConnector.sendVerificationResult(
      authorizationToken,
      scheduledVerification.updateInfo.updateId,
      RequestBody.create(stringMediaType, scheduledVerification.availableIde.version.asString()),
      RequestBody.create(stringMediaType, verificationResult.verificationVerdict),
      RequestBody.create(stringMediaType, verificationResultData.resultType.name)
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
    @Part("verificationVerdict") verificationVerdict: RequestBody,
    @Part("resultType") resultType: RequestBody
  ): Call<ResponseBody>

}

private data class ScheduledVerificationJson(
  @SerializedName("updateId") val updateId: Int,
  @SerializedName("availableIde") val availableIde: AvailableIdeJson,
  @SerializedName("manually") val manually: Boolean
)

private data class AvailableIdeJson(
  @SerializedName("buildNumber") val buildNumber: String,
  @SerializedName("releaseVersion") val releaseVersion: String?
)