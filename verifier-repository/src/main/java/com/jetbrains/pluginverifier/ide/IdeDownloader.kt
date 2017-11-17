package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.misc.extractTo
import com.jetbrains.pluginverifier.misc.makeOkHttpClient
import com.jetbrains.pluginverifier.network.copyInputStreamWithProgress
import com.jetbrains.pluginverifier.network.executeSuccessfully
import com.jetbrains.pluginverifier.repository.downloader.DownloadResult
import com.jetbrains.pluginverifier.repository.downloader.Downloader
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.util.concurrent.TimeUnit

private interface IdeRepositoryConnector {
  @GET
  @Streaming
  fun downloadIde(@Url downloadUrl: String): Call<ResponseBody>
}

class IdeDownloader(private val ideRepository: IdeRepository,
                    private val downloadProgress: (Double) -> Unit) : Downloader<IdeVersion> {

  private val repositoryConnector = Retrofit.Builder()
      .baseUrl(ideRepository.repositoryUrl.trimEnd('/') + '/')
      .client(makeOkHttpClient(false, 5, TimeUnit.MINUTES))
      .build()
      .create(IdeRepositoryConnector::class.java)


  override fun download(key: IdeVersion, tempDirectory: File): DownloadResult {
    val zippedIde = Files.createTempFile(tempDirectory.toPath(), "", ".zip").toFile()
    try {
      val downloadUrl = try {
        ideRepository.fetchAvailableIdeDescriptor(key)?.downloadUrl
      } catch (e: Exception) {
        null
      } ?: return DownloadResult.NotFound("IDE $key is not found in $ideRepository")

      try {
        doDownloadTo(downloadUrl, zippedIde)
      } catch (e: Exception) {
        //todo: provide clearer messages
        return DownloadResult.FailedToDownload("Unable to download $key: ${e.message}", e)
      }

      val destinationDir = Files.createTempDirectory(tempDirectory.toPath(), "").toFile()
      return try {
        zippedIde.extractTo(destinationDir)
        DownloadResult.Downloaded(destinationDir, "")
      } catch (e: Exception) {
        destinationDir.deleteLogged()
        DownloadResult.FailedToDownload("Unable to extract $key zip: ${e.message}", e)
      } catch (e: Throwable) {
        destinationDir.deleteLogged()
        throw e
      }
    } finally {
      zippedIde.deleteLogged()
    }
  }

  private fun doDownloadTo(downloadUrl: URL, destination: File) {
    val response = repositoryConnector.downloadIde(downloadUrl.toExternalForm()).executeSuccessfully()
    val expectedSize = response.body().contentLength()
    response.body().byteStream().use { bodyStream ->
      copyInputStreamWithProgress(bodyStream, expectedSize, destination, downloadProgress)
    }
  }


}