package org.jetbrains.plugins.verifier.service.service.ide

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.jetbrains.pluginverifier.ide.AvailableIde
import com.jetbrains.pluginverifier.misc.createOkHttpClient
import com.jetbrains.pluginverifier.network.createStringRequestBody
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
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

private data class AvailableIdeJson(
    @SerializedName("ideVersion")
    val ideVersion: String,

    @SerializedName("releasedVersion")
    val releasedVersion: String?,

    //todo: remove this when the Plugin Repository is ready. It will be no later than on July 13, 2018 :)
    @SerializedName("isSnapshot")
    val isSnapshot: Boolean = false
)

private fun AvailableIde.convertToJson() = AvailableIdeJson(
    version.asString(),
    releaseVersion
)

private interface AvailableIdeConnector {
  @Multipart
  @POST("/verification/receiveAvailableIdes")
  fun sendAvailableIdes(@Part("userName") userName: RequestBody,
                        @Part("password") password: RequestBody,
                        @Part("availableIdes") availableIdes: List<AvailableIdeJson>): Call<ResponseBody>
}

class DefaultAvailableIdeProtocol(
    authorizationData: AuthorizationData,
    pluginRepository: MarketplaceRepository
) : AvailableIdeProtocol {

  private val userNameRequestBody = createStringRequestBody(authorizationData.pluginRepositoryUserName)

  private val passwordRequestBody = createStringRequestBody(authorizationData.pluginRepositoryPassword)

  private val retrofitConnector by lazy {
    Retrofit.Builder()
        .baseUrl(HttpUrl.get(pluginRepository.repositoryURL))
        .addConverterFactory(GsonConverterFactory.create(Gson()))
        .client(createOkHttpClient(false, 5, TimeUnit.MINUTES))
        .build()
        .create(AvailableIdeConnector::class.java)
  }

  override fun sendAvailableIdes(availableIdes: List<AvailableIde>) {
    val jsonIdes = availableIdes.map { it.convertToJson() }
    retrofitConnector.sendAvailableIdes(
        userNameRequestBody,
        passwordRequestBody,
        jsonIdes
    ).executeSuccessfully().body()
  }

}