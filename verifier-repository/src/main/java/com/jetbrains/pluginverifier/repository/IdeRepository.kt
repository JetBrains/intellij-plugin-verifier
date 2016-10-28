package com.jetbrains.pluginverifier.repository

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.misc.deleteLogged
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.apache.commons.io.FileUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import java.util.concurrent.TimeUnit
import java.util.function.Function

data class AvailableIde(val version: IdeVersion,
                        val isRelease: Boolean,
                        val isCommunity: Boolean,
                        val isSnapshot: Boolean,
                        val downloadUrl: URL)

object IdeRepository {

  private val LOG: Logger = LoggerFactory.getLogger(IdeRepository::class.java)

  private fun <T> Call<T>.executeSuccessfully(): Response<T> {
    val server = "${this.request().url().host()}:${this.request().url().port()}"
    val response: Response<T>?
    try {
      response = this.execute()
    } catch(e: IOException) {
      throw RuntimeException("The server $server is not available", e)
    }
    if (response.isSuccessful) {
      return response
    }
    if (response.code() == 500) {
      throw RuntimeException("The server $server has faced unexpected problems (500 Internal Server Error)")
    }
    throw RuntimeException("The response status code is ${response.code()}: ${response.errorBody().string()}")
  }

  private fun parseDocument(document: Document, snapshots: Boolean): List<AvailableIde> {
    val table = document.getElementsByTag("table")[0]
    val tbody = table.getElementsByTag("tbody")[0]
    val tableRows = tbody.getElementsByTag("tr")

    val result = arrayListOf<AvailableIde>()

    tableRows.forEach {
      val columns = it.getElementsByTag("td")

      val version = columns[0].text().trim()
      val buildNumberString = columns[2].text().trim()
      val buildNumber = IdeVersion.createIdeVersion(buildNumberString)
      val isRelease = version != buildNumberString

      val artifacts = columns[3]
      val ideaIU = artifacts.getElementsContainingOwnText("ideaIU.zip")
      if (ideaIU.isNotEmpty()) {
        val downloadIdeaIU = URL(ideaIU[0].attr("href"))
        result.add(AvailableIde(buildNumber, isRelease, false, snapshots, downloadIdeaIU))
      }

      val ideaIC = artifacts.getElementsContainingOwnText("ideaIC.zip")
      if (ideaIC.isNotEmpty()) {
        val downloadIdeaIC = URL(ideaIC[0].attr("href"))
        result.add(AvailableIde(buildNumber, isRelease, true, snapshots, downloadIdeaIC))
      }
    }

    return result
  }

  fun fetchIndex(snapshots: Boolean = false): List<AvailableIde> {
    val repoUrl = RepositoryConfiguration.ideRepositoryUrl.trimEnd('/') + "/intellij-repository/" + (if (snapshots) "snapshots" else "releases") + "/"
    try {
      val document = Jsoup.connect(repoUrl).get()
      return parseDocument(document, snapshots)
    } catch (e: Exception) {
      LOG.error("Unable to fetch repository $repoUrl index", e)
      throw e
    }
  }

  fun downloadIde(availableIde: AvailableIde,
                  saveTo: File,
                  progress: Function<Double, Unit>? = null): File {
    if (saveTo.exists()) {
      try {
        FileUtils.forceDelete(saveTo)
      } catch (e: Exception) {
        throw IOException("Unable to delete the existing file to which the IDE should be saved", e)
      }
    }

    val downloadUrl = availableIde.downloadUrl
    val connection: URLConnection
    try {
      connection = downloadUrl.openConnection()
      connection.readTimeout = TimeUnit.HOURS.toMillis(1).toInt()
      connection.connectTimeout = TimeUnit.HOURS.toMillis(1).toInt()
    } catch (e: Exception) {
      saveTo.deleteLogged()
      LOG.error("Unable to download IDE by $downloadUrl", e)
      throw e
    }

    try {
      val fileSize = connection.contentLengthLong
      connection.inputStream.buffered().use { inputStream ->
        copyInputStreamWithProgress(inputStream, fileSize, saveTo, progress)
      }

      return saveTo
    } catch (e: Exception) {
      saveTo.deleteLogged()
      LOG.error("Unable to save the IDE #${availableIde.version} (snapshot = ${availableIde.isSnapshot}) (community = ${availableIde.isCommunity})", e)
      throw e
    }
  }

  private fun getRepo(fromSnapshots: Boolean) = if (fromSnapshots) "snapshots" else "releases"

  private fun copyInputStreamWithProgress(inputStream: InputStream, fileSize: Long, toFile: File, progress: Function<Double, Unit>?) {
    val buffer = ByteArray(4 * 1024)
    if (fileSize == 0L) {
      throw IllegalArgumentException("File is empty")
    }

    inputStream.use { input ->
      toFile.outputStream().use { output ->
        var count: Long = 0
        var n: Int
        while (true) {
          n = input.read(buffer)
          if (n == -1) break
          output.write(buffer, 0, n)
          count += n
          progress?.apply(count.toDouble() / fileSize)
        }
      }
    }
  }

  private val repository: RepositoryInterface = Retrofit.Builder()
      .baseUrl(RepositoryConfiguration.ideRepositoryUrl.trimEnd('/') + '/')
      .addConverterFactory(GsonConverterFactory.create(Gson()))
      .client(makeClient())
      .build()
      .create(RepositoryInterface::class.java)

  private fun makeClient(): OkHttpClient = OkHttpClient.Builder()
      .connectTimeout(1, TimeUnit.HOURS)
      .readTimeout(1, TimeUnit.HOURS)
      .writeTimeout(1, TimeUnit.HOURS)
      .addInterceptor(HttpLoggingInterceptor().setLevel(if (LOG.isDebugEnabled) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE))
      .build()

}

private data class RepositoryIndex(@SerializedName("artifacts") val artifacts: List<ArtifactIndex>)

private data class ArtifactIndex(@SerializedName("groupId") val groupId: String,
                                 @SerializedName("artifactId") val artifactId: String,
                                 @SerializedName("version") val version: String,
                                 @SerializedName("classifier") val classifier: String,
                                 @SerializedName("packaging") val packaging: String,
                                 @SerializedName("lastModified") val lastModified: String,
                                 @SerializedName("lastModifiedUnixTimeMs") val lastModifiedUnixTimeMs: Long,
                                 @SerializedName("eTag") val eTag: String)


private interface RepositoryInterface {

  @Streaming
  @GET("/intellij-repository/{repo}/com/jetbrains/intellij/idea/{ideaName}/{ideVersion}/{ideaName}-{ideVersion}.zip")
  fun downloadFrom(@Path("repo") repo: String, @Path("ideaName") ideaName: String, @Path("ideVersion") ideVersion: String): Call<ResponseBody>

  @GET("/intellij-repository/{repo}/index.json")
  fun fetchIndex(@Path("repo") repo: String): Call<RepositoryIndex>

}