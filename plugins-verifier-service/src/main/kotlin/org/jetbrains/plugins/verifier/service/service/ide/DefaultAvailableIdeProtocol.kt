package org.jetbrains.plugins.verifier.service.service.ide

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.jetbrains.plugin.structure.ide.IntelliJPlatformProduct
import com.jetbrains.pluginverifier.ide.AvailableIde
import com.jetbrains.pluginverifier.misc.createOkHttpClient
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import okhttp3.HttpUrl
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

private data class AvailableIdeJson(
  @SerializedName("ideVersion")
  val ideVersion: String,

  @SerializedName("releaseVersion")
  val releaseVersion: String?,

  @SerializedName("productName")
  val productName: String
)

private fun AvailableIde.convertToJson() = AvailableIdeJson(
  version.asString(),
  releaseVersion,
  IntelliJPlatformProduct.fromIdeVersion(version)?.productName ?: IntelliJPlatformProduct.IDEA.productName
)

private interface AvailableIdeConnector {
  @POST("/verification/receiveAvailableIdes")
  fun sendAvailableIdes(
    @Header("Authorization") authorization: String,
    @Body availableIdes: List<AvailableIdeJson>
  ): Call<ResponseBody>
}

class DefaultAvailableIdeProtocol(
  token: String,
  pluginRepository: MarketplaceRepository
) : AvailableIdeProtocol {
  private val authorizationToken = "Bearer $token"

  private val retrofitConnector by lazy {
    Retrofit.Builder()
      .baseUrl(HttpUrl.get(pluginRepository.repositoryURL)!!)
      .addConverterFactory(GsonConverterFactory.create(Gson()))
      .client(createOkHttpClient(false, 5, TimeUnit.MINUTES))
      .build()
      .create(AvailableIdeConnector::class.java)
  }

  override fun sendAvailableIdes(availableIdes: List<AvailableIde>) {
    val jsonIdes = availableIdes.map { it.convertToJson() }
    retrofitConnector.sendAvailableIdes(
      authorizationToken,
      jsonIdes
    ).executeSuccessfully().body()
  }

}