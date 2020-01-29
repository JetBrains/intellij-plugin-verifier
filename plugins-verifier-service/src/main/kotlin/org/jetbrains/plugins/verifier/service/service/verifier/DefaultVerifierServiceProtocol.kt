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
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

class DefaultVerifierServiceProtocol(
  token: String,
  private val pluginRepository: MarketplaceRepository,
  private val ideRepository: IdeRepository
) : VerifierServiceProtocol {

  private val retrofitConnector: VerifierRetrofitConnector by lazy {
    Retrofit.Builder()
      .baseUrl(pluginRepository.repositoryURL.toHttpUrlOrNull()!!)
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
      .executeSuccessfully().body()!!
      .mapNotNull { buildScheduledVerification(it) }

  private fun buildScheduledVerification(json: ScheduledVerificationJson): ScheduledVerification? {
    val ideVersion = IdeVersion.createIdeVersionIfValid(json.availableIde.ideVersion) ?: return null
    val availableIde = ideRepository.fetchAvailableIde(ideVersion) ?: return null
    val updateInfo = pluginRepository.getPluginInfoById(json.updateId) ?: return null
    return ScheduledVerification(updateInfo, availableIde, json.manually)
  }

  override fun sendVerificationResult(scheduledVerification: ScheduledVerification, verificationResult: PluginVerificationResult) {
    val verificationResultData = verificationResult.prepareResponse(scheduledVerification)

    val ideVersion = scheduledVerification.availableIde.version.asString()
    val updateId = scheduledVerification.updateInfo.updateId

    val addResponse = retrofitConnector.addVerificationResult(
      authorizationToken,
      updateId,
      ideVersion.toRequestBody(stringMediaType),
      verificationResult.verificationVerdict.toRequestBody(stringMediaType),
      verificationResultData.resultType.name.toRequestBody(stringMediaType)
    ).executeSuccessfully()

    if (addResponse.code() == HttpURLConnection.HTTP_ACCEPTED) {
      return
    }

    val uploadUrl = addResponse.body()!!.string()

    retrofitConnector.uploadVerificationResult(
      uploadUrl,
      json.toJson(verificationResultData).toRequestBody(jsonMediaType)
    ).executeSuccessfully()

    retrofitConnector.saveVerificationResult(
      authorizationToken,
      updateId,
      ideVersion.toRequestBody(stringMediaType)
    ).executeSuccessfully()
  }

}

private interface VerifierRetrofitConnector {

  @GET("/verification/getScheduledVerifications")
  fun getScheduledVerifications(@Header("Authorization") authorization: String): Call<List<ScheduledVerificationJson>>

  @POST("/verification/addVerificationResult")
  @Multipart
  fun addVerificationResult(
    @Header("Authorization") authorization: String,
    @Part("updateId") updateId: Int,
    @Part("ideVersion") ideVersion: RequestBody,
    @Part("verificationVerdict") verificationVerdict: RequestBody,
    @Part("resultType") resultType: RequestBody
  ): Call<ResponseBody>

  @PUT
  fun uploadVerificationResult(
    @Url url: String,
    @Body content: RequestBody
  ): Call<ResponseBody>

  @POST("/verification/saveVerificationResult")
  @Multipart
  fun saveVerificationResult(
    @Header("Authorization") authorization: String,
    @Part("updateId") updateId: Int,
    @Part("ideVersion") ideVersion: RequestBody
  ): Call<ResponseBody>
}

data class VerificationResultUploadUrl(
  @SerializedName("resultId") val resultId: Int,
  @SerializedName("uploadUrl") val uploadUrl: String
)

private data class ScheduledVerificationJson(
  @SerializedName("updateId") val updateId: Int,
  @SerializedName("availableIde") val availableIde: AvailableIdeJson,
  @SerializedName("manually") val manually: Boolean
)

private data class AvailableIdeJson(
  @SerializedName("ideVersion") val ideVersion: String,
  @SerializedName("releaseVersion") val releaseVersion: String?
)