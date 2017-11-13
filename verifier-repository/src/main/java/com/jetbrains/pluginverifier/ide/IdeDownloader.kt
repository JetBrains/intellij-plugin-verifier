package com.jetbrains.pluginverifier.ide

import com.google.gson.Gson
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.misc.extractTo
import com.jetbrains.pluginverifier.misc.makeOkHttpClient
import com.jetbrains.pluginverifier.network.executeSuccessfully
import okhttp3.ResponseBody
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.util.concurrent.TimeUnit

class IdeDownloader(repositoryUrl: String, private val downloadDir: File) {

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(IdeRepository::class.java)
  }

  private interface IdeRepositoryConnector {

    @GET
    @Streaming
    fun downloadIde(@Url downloadUrl: String): Call<ResponseBody>

  }

  private val repositoryConnector: IdeRepositoryConnector = Retrofit.Builder()
      .baseUrl(repositoryUrl.trimEnd('/') + '/')
      .addConverterFactory(GsonConverterFactory.create(Gson()))
      .client(makeOkHttpClient(false, 5, TimeUnit.MINUTES))
      .build()
      .create(IdeRepositoryConnector::class.java)


  private fun getIdeDirForVersion(ideVersion: IdeVersion) = File(downloadDir, ideVersion.toString())

  private fun downloadAndExtractIde(availableIde: AvailableIde, progress: (Double) -> Unit): File {
    val tempIdeZip = File.createTempFile("ide-${availableIde.version}", ".zip", downloadDir)
    try {
      return downloadIdeToTempZipAndExtract(availableIde, tempIdeZip, progress)
    } finally {
      tempIdeZip.deleteLogged()
    }
  }

  private fun downloadIdeToTempZipAndExtract(availableIde: AvailableIde, tempZip: File, progress: (Double) -> Unit): File {
    downloadIdeZipTo(availableIde, tempZip, progress)

    val tempIdeDir = Files.createTempDirectory(downloadDir.toPath(), "ide").toFile()
    try {
      return extractIdeZipToTempDirectoryAndMove(tempZip, tempIdeDir, availableIde)
    } finally {
      tempIdeDir.deleteLogged()
    }
  }

  private fun extractIdeZipToTempDirectoryAndMove(tempZip: File, tempIdeDir: File, availableIde: AvailableIde): File {
    tempZip.extractTo(tempIdeDir)
    synchronized(this) {
      val ideDir = getIdeDirForVersion(availableIde.version)
      if (!ideDir.exists()) {
        FileUtils.moveDirectory(tempIdeDir, ideDir)
      }
      return ideDir
    }
  }

  fun getOrDownloadIde(availableIde: AvailableIde, progress: (Double) -> Unit): File {
    val ideVersion = availableIde.version
    val ideDir = getIdeDirForVersion(ideVersion)
    return if (ideDir.isDirectory && ideDir.list().orEmpty().isNotEmpty()) {
      LOG.info("IDE #$ideVersion is found in $ideDir")
      ideDir
    } else {
      LOG.info("Downloading IDE $availableIde")
      val downloadedIde = downloadAndExtractIde(availableIde, progress)
      LOG.info("Successfully downloaded to $downloadedIde")
      downloadedIde
    }
  }


  private fun downloadIdeZipTo(availableIde: AvailableIde, saveTo: File, progress: ((Double) -> Unit)) {
    val downloadUrl = availableIde.downloadUrl.toExternalForm()
    val response = repositoryConnector.downloadIde(downloadUrl).executeSuccessfully()
    val expectedSize = response.body().contentLength()
    val stream = response.body().byteStream()
    stream.use { inputStream ->
      copyInputStreamWithProgress(inputStream, expectedSize, saveTo, progress)
    }
  }

  private fun copyInputStreamWithProgress(inputStream: InputStream,
                                          expectedSize: Long,
                                          toFile: File,
                                          progress: (Double) -> Unit) {
    val buffer = ByteArray(4 * 1024)
    if (expectedSize == 0L) {
      throw IllegalArgumentException("File is empty")
    }

    inputStream.use { input ->
      toFile.outputStream().buffered().use { output ->
        var count: Long = 0
        var n: Int
        while (true) {
          n = input.read(buffer)
          if (n == -1) break
          output.write(buffer, 0, n)
          count += n
          progress(count.toDouble() / expectedSize)
        }
      }
    }
  }

}

