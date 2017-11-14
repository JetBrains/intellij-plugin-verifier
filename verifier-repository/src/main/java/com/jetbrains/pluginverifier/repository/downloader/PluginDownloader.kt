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

    val TEMP_PLUGIN_DOWNLOAD_PREFIX = "download-plugin"
  }

  private val repositoryDownloadConnector = Retrofit.Builder()
      .baseUrl(pluginRepositoryUrl.trimEnd('/') + '/')
      .client(makeOkHttpClient(false, 5, TimeUnit.MINUTES))
      .build()
      .create(PluginDownloadConnector::class.java)

  override fun download(destinationDirectory: File, key: UpdateId): DownloadResult {
    return try {
      doDownloadToTempFile(key.id, destinationDirectory)
    } catch (e: NotFound404ResponseException) {
      DownloadResult.NotFound("Plugin $key is not found in the Plugin Repository $pluginRepositoryUrl")
    } catch (e: Exception) {
      //todo: provide a human readable error message: maybe find a library capable of this?
      val message = "Unable to download plugin $key" + if (e.message.isNullOrBlank()) "" else ": " + e.message
      LOG.debug(message, e)
      DownloadResult.FailedToDownload(message, e)
    }
  }

  private fun doDownloadToTempFile(updateId: Int, destinationDirectory: File): DownloadResult.Downloaded {
    val response = repositoryDownloadConnector.downloadPlugin(updateId).executeSuccessfully()
    val extension = response.guessExtension()
    val prefix = "$TEMP_PLUGIN_DOWNLOAD_PREFIX-$updateId"
    val suffix = if (extension.isNotEmpty()) "." + extension else ""
    val tempFile = Files.createTempFile(destinationDirectory.toPath(), prefix, suffix).toFile()
    LOG.debug("Downloading plugin #$updateId to $tempFile")
    return try {
      FileUtils.copyInputStreamToFile(response.body().byteStream(), tempFile)
      DownloadResult.Downloaded(tempFile, extension)
    } catch (e: Exception) {
      tempFile.deleteLogged()
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