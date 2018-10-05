package org.jetbrains.plugins.verifier.service.service.verifier

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.createOkHttpClient
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.jetbrains.plugins.verifier.service.setting.AuthorizationData
import org.jetbrains.plugins.verifier.service.setting.Settings
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit

class DefaultVerifierServiceProtocol(
    authorizationData: AuthorizationData,
    private val s3Client: AmazonS3,
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
  private val s3BucketName = Settings.AWS_BUCKET_NAME.get()
  private val s3BucketPrefix = Settings.AWS_BUCKET_PREFIX.get()

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
    val key = "${updateInfo.pluginIntId}/${updateInfo.updateId}/verification/${verificationResponse.ideVersion}.bin"
    val verificationResponseContent = verificationResponse.toByteArray()
    val metadata = ObjectMetadata()
    metadata.contentLength = verificationResponseContent.size.toLong()
    s3Client.putObject(
        s3BucketName, "$s3BucketPrefix$key", ByteArrayInputStream(verificationResponseContent), metadata
    )
    retrofitConnector.sendVerificationResult(
        authorizationToken,
        updateInfo.updateId,
        RequestBody.create(MediaType.parse("text/plain"), verificationResponse.ideVersion),
        RequestBody.create(MediaType.parse("text/plain"), verificationResult.verificationVerdict),
        RequestBody.create(MediaType.parse("text/plain"), verificationResponse.resultType.name)
    ).executeSuccessfully()
  }

}

private interface VerifierRetrofitConnector {

  @GET("/verification/getScheduledVerifications")
  fun getScheduledVerifications(@Header("Authorization") authorization: String): Call<List<ScheduledVerificationJson>>

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