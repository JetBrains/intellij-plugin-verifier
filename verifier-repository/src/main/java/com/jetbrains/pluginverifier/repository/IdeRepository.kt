package com.jetbrains.pluginverifier.repository

import com.google.gson.Gson
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.misc.extractTo
import com.jetbrains.pluginverifier.misc.makeOkHttpClient
import com.jetbrains.pluginverifier.network.executeSuccessfully
import okhttp3.ResponseBody
import org.apache.commons.io.FileUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
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
import java.net.URL
import java.nio.file.Files
import java.util.concurrent.TimeUnit

data class AvailableIde(val version: IdeVersion,
                        val isRelease: Boolean,
                        val isCommunity: Boolean,
                        val isSnapshot: Boolean,
                        val downloadUrl: URL) {
  override fun toString(): String = version.toString() + if (isSnapshot) " (snapshot)" else ""
}

class IdeRepository(private val downloadDir: File, private val repositoryUrl: String) {

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(IdeRepository::class.java)
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
        val fullVersion = setFullProductNameIfNecessary(buildNumber, "IU")
        result.add(AvailableIde(fullVersion, isRelease, false, snapshots, downloadIdeaIU))
      }

      val ideaIC = artifacts.getElementsContainingOwnText("ideaIC.zip")
      if (ideaIC.isNotEmpty()) {
        val downloadIdeaIC = URL(ideaIC[0].attr("href"))
        val fullVersion = setFullProductNameIfNecessary(buildNumber, "IC")
        result.add(AvailableIde(fullVersion, isRelease, true, snapshots, downloadIdeaIC))
      }
    }

    return result
  }

  private fun setFullProductNameIfNecessary(ideVersion: IdeVersion, productName: String): IdeVersion =
      if (ideVersion.productCode.isEmpty())
        IdeVersion.createIdeVersion("$productName-" + ideVersion.asStringWithoutProductCode())
      else {
        ideVersion
      }

  fun fetchIndex(snapshots: Boolean = false): List<AvailableIde> {
    val repoUrl = repositoryUrl.trimEnd('/') + "/intellij-repository/" + (if (snapshots) "snapshots" else "releases") + "/"
    try {
      val document = Jsoup
          .connect(repoUrl)
          .timeout(3000)
          .get()
      return parseDocument(document, snapshots)
    } catch (e: Exception) {
      LOG.error("Unable to fetch repository $repoUrl index", e)
      throw e
    }
  }

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

  fun getOrDownloadIde(ideVersion: IdeVersion, progressUpdater: (Double) -> Unit): File {
    val fromReleases = fetchIndex(snapshots = false).find { it.version == ideVersion }
    if (fromReleases != null) {
      return getOrDownloadIde(fromReleases, progressUpdater)
    }

    val fromSnapshots = fetchIndex(snapshots = true).find { it.version == ideVersion }
    if (fromSnapshots != null) {
      return getOrDownloadIde(fromSnapshots, progressUpdater)
    }

    if (ideVersion.productCode.isEmpty()) {
      throw IllegalArgumentException("Please specify product code of the IDE build $ideVersion: either IU-$ideVersion or IC-$ideVersion")
    }

    throw IllegalArgumentException("IDE #$ideVersion is not found neither in https://www.jetbrains.com/intellij-repository/releases/ nor in https://www.jetbrains.com/intellij-repository/releases/")
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
    val response = repository.downloadIde(downloadUrl).executeSuccessfully()
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

  override fun toString(): String = "IDE Repository on $repositoryUrl"

  private val repository: IdeRepositoryApi = Retrofit.Builder()
      .baseUrl(repositoryUrl.trimEnd('/') + '/')
      .addConverterFactory(GsonConverterFactory.create(Gson()))
      .client(makeOkHttpClient(false, 5, TimeUnit.MINUTES))
      .build()
      .create(IdeRepositoryApi::class.java)

}

private interface IdeRepositoryApi {

  @GET
  @Streaming
  fun downloadIde(@Url downloadUrl: String): Call<ResponseBody>

}