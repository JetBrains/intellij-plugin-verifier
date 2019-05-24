package com.jetbrains.pluginverifier.ide.repositories

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.AvailableIde
import com.jetbrains.pluginverifier.misc.createOkHttpClient
import com.jetbrains.pluginverifier.network.executeSuccessfully
import okhttp3.ResponseBody
import org.bouncycastle.cms.CMSSignedData
import org.codehaus.plexus.archiver.xz.XZUnArchiver
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.net.URL
import java.util.concurrent.TimeUnit

class AndroidStudioIdeRepository : IdeRepository {

  private companion object {
    val feedUrl = URL("https://download.jetbrains.com/toolbox/feeds/v1/android-studio.feed.xz.signed")
    val jsonParser = Gson()
  }

  private val feedConnector by lazy {
    Retrofit.Builder()
        .baseUrl("https://unused.com")
        .client(createOkHttpClient(false, 5, TimeUnit.MINUTES))
        .build()
        .create(FeedConnector::class.java)
  }

  override fun fetchIndex(): List<AvailableIde> {
    val responseBody = feedConnector.getFeed(feedUrl.toExternalForm()).executeSuccessfully().body()
    val feed = responseBody.use {
      val signedContent = CMSSignedData(responseBody.byteStream()).signedContent.content as ByteArray
      jsonParser.fromJson(XZUnArchiver.getXZInputStream(signedContent.inputStream()).reader(), Feed::class.java)
    }
    return feed.entries
        .filter { it.packageInfo.type == "zip" }
        .map {
          AvailableIde(
              IdeVersion.createIdeVersion(it.build).setProductCodeIfAbsent("AI"),
              it.version,
              it.packageInfo.url
          )
        }
  }
}

private interface FeedConnector {

  @GET
  @Streaming
  fun getFeed(@Url url: String): Call<ResponseBody>

}


private data class Feed(@SerializedName("entries") val entries: List<FeedEntry>)

private data class FeedEntry(
    @SerializedName("build")
    val build: String,

    @SerializedName("version")
    val version: String,

    @SerializedName("package")
    val packageInfo: PackageInfo
)

private data class PackageInfo(
    @SerializedName("type")
    val type: String,

    @SerializedName("url")
    val url: URL
)
