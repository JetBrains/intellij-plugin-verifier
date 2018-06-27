package com.jetbrains.pluginverifier.repository.downloader

import com.jetbrains.pluginverifier.misc.*
import com.jetbrains.pluginverifier.network.*
import okhttp3.ResponseBody
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * [Downloader] that can download files by URLs provided with [urlProvider].
 */
class UrlDownloader<in K>(private val urlProvider: (K) -> URL?) : Downloader<K> {

  private companion object {
    private const val FILE_PROTOCOL = "file"
    private const val HTTP_PROTOCOL = "http"
    private const val HTTPS_PROTOCOL = "https"

    private val LOG = LoggerFactory.getLogger(UrlDownloader::class.java)
  }

  private val downloadConnector: DownloadConnector = Retrofit.Builder()
      //the base repository is not used because all URLs provided by [urlProvider] are absolute
      .baseUrl("http://unnecessary.com")
      .client(makeOkHttpClient(LOG.isDebugEnabled, 5, TimeUnit.MINUTES))
      .build()
      .create(DownloadConnector::class.java)

  //todo: provide more guarantees here.
  private fun Response<ResponseBody>.guessExtension() =
      when (body().contentType()) {
        jarContentMediaType, xJarContentMediaType -> "jar"
        jsonMediaType -> "json"
        else -> if (raw().request().url().encodedPath().endsWith(".jar")) {
          "jar"
        } else {
          "zip"
        }
      }

  override fun download(key: K, tempDirectory: Path): DownloadResult {
    val downloadUrl = try {
      urlProvider(key)
    } catch (e: Exception) {
      return DownloadResult.FailedToDownload("Invalid URL", e)
    } ?: return DownloadResult.NotFound("Unknown URL for $key")

    return downloadByUrl(key, downloadUrl, tempDirectory)
  }

  private fun downloadByUrl(key: K, downloadUrl: URL, tempDirectory: Path): DownloadResult {
    checkIfInterrupted()
    return try {
      doDownload(key, downloadUrl, tempDirectory)
    } catch (e: NotFound404ResponseException) {
      DownloadResult.NotFound("Resource is not found by $downloadUrl")
    } catch (e: Exception) {
      DownloadResult.FailedToDownload("Unable to download $key: ${e.message}", e)
    }
  }

  private fun doDownload(key: K, downloadUrl: URL, tempDirectory: Path): DownloadResult {
    val protocol = downloadUrl.protocol
    return when (protocol) {
      FILE_PROTOCOL -> copyFileOrDirectory(downloadUrl, tempDirectory)
      HTTP_PROTOCOL, HTTPS_PROTOCOL -> downloadFileOrDirectory(downloadUrl, tempDirectory, key)
      else -> throw IllegalArgumentException("Unknown protocol: $protocol of $downloadUrl")
    }
  }

  private fun copyFileOrDirectory(downloadUrl: URL, tempDirectory: Path): DownloadResult.Downloaded {
    val original = FileUtils.toFile(downloadUrl).toPath()
    val destination = tempDirectory.resolve(original.simpleName)
    original.toFile().copyRecursively(destination.toFile())
    return DownloadResult.Downloaded(destination, destination.extension, destination.isDirectory)
  }

  private fun downloadFileOrDirectory(downloadUrl: URL, tempDirectory: Path, key: K): DownloadResult {
    val response = downloadConnector.download(downloadUrl.toExternalForm()).executeSuccessfully()
    val extension = response.guessExtension()
    val downloadedTempFile = Files.createTempFile(tempDirectory, "", ".$extension")
    return try {
      LOG.debug("Downloading $key to $downloadedTempFile")
      copyResponseTo(response, downloadedTempFile)
      DownloadResult.Downloaded(downloadedTempFile, extension, false)
    } catch (e: Throwable) {
      downloadedTempFile.deleteLogged()
      throw e
    }
  }

  private fun copyResponseTo(response: Response<ResponseBody>, file: Path) {
    checkIfInterrupted()
    response.body().use { responseBody ->
      val expectedSize = responseBody.contentLength()
      copyInputStreamToFileWithProgress(responseBody.byteStream(), expectedSize, file.toFile(), { })
    }
  }

  private interface DownloadConnector {
    @Streaming
    @GET
    fun download(@Url url: String): Call<ResponseBody>
  }

}