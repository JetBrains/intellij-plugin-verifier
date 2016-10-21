package com.jetbrains.pluginverifier.repository

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.jetbrains.pluginverifier.misc.deleteLogged
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.apache.commons.io.FileUtils
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
import java.util.concurrent.TimeUnit
import java.util.function.Function

/**
 * Requires a [String] instead of [com.intellij.structure.domain.IdeVersion],
 * because the https://www.jetbrains.com/intellij-repository/snapshots/
 * contains also invalid [IdeVersion]'s: 163.4396-EAP-CANDIDATE-SNAPSHOT,
 * 162.646-EAP-CANDIDATE-SNAPSHOT
 */
data class AvailableIde(val version: String,
                        val isCommunity: Boolean,
                        val snapshots: Boolean)

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

  fun fetchIndex(snapshots: Boolean = false): List<AvailableIde> {
    val call = repository.fetchIndex(getRepo(snapshots))
    try {
      return call.executeSuccessfully().body()
          .artifacts
          .filter { it.artifactId == "ideaIU" || it.artifactId == "ideaIC" }
          .filter { it.packaging == "zip" }
          .map { AvailableIde(it.version, it.artifactId == "ideaIC", snapshots) }
    } catch (e: Exception) {
      LOG.error("Unable to fetch repository ${call.request().url()} index", e)
      throw e
    }
  }

  fun downloadIde(availableIde: AvailableIde,
                  saveTo: File,
                  progress: Function<Double, Unit>? = null): File {
    val ideaName = if (availableIde.isCommunity) "ideaIC" else "ideaIU"
    val call = repository.downloadFrom(getRepo(availableIde.snapshots), ideaName, availableIde.version)
    val body: ResponseBody
    try {
      body = call.executeSuccessfully().body()
    } catch (e: Exception) {
      LOG.error("Unable to download #${availableIde.version} (snapshot = ${availableIde.snapshots}) (community = ${availableIde.isCommunity})", e)
      throw e
    }

    if (saveTo.exists()) {
      try {
        FileUtils.forceDelete(saveTo)
      } catch (e: Exception) {
        throw IOException("Unable to delete the existing file to which the IDE should be saved", e)
      }
    }

    try {
      copyInputStreamWithProgress(body, saveTo, progress)
      return saveTo
    } catch (e: Exception) {
      saveTo.deleteLogged()
      LOG.error("Unable to save the IDE #${availableIde.version} (snapshot = ${availableIde.snapshots}) (community = ${availableIde.isCommunity})", e)
      throw e
    }
  }

  private fun getRepo(fromSnapshots: Boolean) = if (fromSnapshots) "snapshots" else "releases"

  private fun copyInputStreamWithProgress(body: ResponseBody, file: File, progress: Function<Double, Unit>?) {
    val buffer = ByteArray(4 * 1024)
    val fileSize = body.contentLength()
    if (fileSize == 0L) {
      throw IllegalArgumentException("File is empty")
    }

    body.byteStream().use { input ->
      file.outputStream().use { output ->
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