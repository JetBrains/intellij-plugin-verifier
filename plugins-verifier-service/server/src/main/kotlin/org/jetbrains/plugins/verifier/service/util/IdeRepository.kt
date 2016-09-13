package org.jetbrains.plugins.verifier.service.util

import com.jetbrains.pluginverifier.persistence.GsonHolder
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.jetbrains.plugins.verifier.service.core.Progress
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.storage.FileManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Patrikeev
 */
object IdeRepository {

  private val LOG: Logger = LoggerFactory.getLogger(IdeRepository::class.java)

  private val IDE_REPOSITORY_URL: String by lazy {
    Settings.IDE_REPOSITORY_URL.get()
  }

  fun download(ideVersion: String, progress: Progress, isCommunity: Boolean = false, fromSnapshots: Boolean = false): File {
    val ideaName = if (isCommunity) "ideaIC" else "ideaIU"
    val call = if (fromSnapshots) repository.downloadSnapshot(ideaName, ideVersion) else repository.downloadRelease(ideaName, ideVersion)
    val body: ResponseBody
    try {
      body = call.executeSuccessfully().body()
    } catch (e: Exception) {
      val msg = "Unable to download #$ideVersion (snapshot = $fromSnapshots) (community = $isCommunity)"
      LOG.error(msg, e)
      throw e
    }

    val tempFile = FileManager.createTempFile(".zip")
    try {
      copyInputStreamWithProgress(body, tempFile, progress)
      return tempFile
    } catch (e: Exception) {
      tempFile.deleteLogged()
      LOG.error("Unable to save the IDE #$ideVersion (snapshot = $fromSnapshots) (community = $isCommunity)", e)
      throw e
    }
  }

  private fun copyInputStreamWithProgress(body: ResponseBody, file: File, progress: Progress) {
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
          progress.setProgress(count.toDouble() / fileSize)
        }
      }
    }
  }

  private val repository: RepositoryInterface = Retrofit.Builder()
      .baseUrl(IDE_REPOSITORY_URL.trimEnd('/') + '/')
      .addConverterFactory(GsonConverterFactory.create(GsonHolder.GSON))
      .client(makeClient())
      .build()
      .create(RepositoryInterface::class.java)

  private fun makeClient(): OkHttpClient = OkHttpClient.Builder()
      .connectTimeout(5, TimeUnit.MINUTES)
      .readTimeout(5, TimeUnit.MINUTES)
      .writeTimeout(5, TimeUnit.MINUTES)
      .addInterceptor(HttpLoggingInterceptor().setLevel(if (LOG.isDebugEnabled) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE))
      .build()

}

interface RepositoryInterface {

  @Streaming
  @GET("/intellij-repository/releases/com/jetbrains/intellij/idea/{ideaName}/{ideVersion}/{ideaName}-{ideVersion}.zip")
  fun downloadRelease(@Path("ideaName") ideaName: String, @Path("ideVersion") ideVersion: String): Call<ResponseBody>

  @Streaming
  @GET("/intellij-repository/snapshots/com/jetbrains/intellij/idea/{ideaName}/{ideVersion}/{ideaName}-{ideVersion}.zip")
  fun downloadSnapshot(@Path("ideaName") ideaName: String, @Path("ideVersion") ideVersion: String): Call<ResponseBody>

}