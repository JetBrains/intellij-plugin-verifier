package com.jetbrains.pluginverifier.repository.downloader

import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.misc.makeOkHttpClient
import com.jetbrains.pluginverifier.network.NotFound404ResponseException
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.network.jarContentMediaType
import com.jetbrains.pluginverifier.repository.UpdateId
import okhttp3.ResponseBody
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

internal interface PluginDownloadConnector {
  @GET("/plugin/download/?noStatistic=true")
  @Streaming
  fun downloadPlugin(@Query("updateId") updateId: Int): Call<ResponseBody>
}

class PluginDownloader(private val pluginRepositoryUrl: String) : Downloader<UpdateId> {

  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(PluginDownloader::class.java)
  }

  private val repositoryDownloadConnector = Retrofit.Builder()
      .baseUrl(pluginRepositoryUrl.trimEnd('/') + '/')
      .client(makeOkHttpClient(false, 5, TimeUnit.MINUTES))
      .build()
      .create(PluginDownloadConnector::class.java)

  override fun download(key: UpdateId, tempDirectory: File): DownloadResult = try {
    doDownload(key, tempDirectory)
  } catch (e: NotFound404ResponseException) {
    DownloadResult.NotFound("Plugin $key is not found in the Plugin Repository $pluginRepositoryUrl")
  } catch (e: Exception) {
    //todo: provide a human readable error message: maybe find a library capable of this?
    val message = "Unable to download plugin $key" + if (e.message.isNullOrBlank()) "" else ": " + e.message
    LOG.debug(message, e)
    DownloadResult.FailedToDownload(message, e)
  }

  private fun doDownload(updateId: UpdateId, tempDirectory: File): DownloadResult.Downloaded {
    val response = repositoryDownloadConnector.downloadPlugin(updateId.id).executeSuccessfully()
    val extension = response.guessExtension()
    val downloadedTempFile = Files.createTempFile(tempDirectory.toPath(), "${updateId.id}", ".$extension").toFile()
    return try {
      LOG.debug("Downloading plugin $updateId to $tempDirectory")
      FileUtils.copyInputStreamToFile(response.body().byteStream(), downloadedTempFile)
      DownloadResult.Downloaded(downloadedTempFile, extension)
    } catch (e: Throwable) {
      downloadedTempFile.deleteLogged()
      throw e
    }
  }

  private fun Response<ResponseBody>.guessExtension(): String =
      if (body().contentType() == jarContentMediaType) {
        "jar"
      } else {
        "zip"
      }
}