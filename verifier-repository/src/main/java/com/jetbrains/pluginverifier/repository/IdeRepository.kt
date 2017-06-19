package com.jetbrains.pluginverifier.repository

import com.google.gson.Gson
import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.misc.executeSuccessfully
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
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.concurrent.TimeUnit

data class AvailableIde(val version: IdeVersion,
                        val isRelease: Boolean,
                        val isCommunity: Boolean,
                        val isSnapshot: Boolean,
                        val downloadUrl: URL)

object IdeRepository {

  private val LOG: Logger = LoggerFactory.getLogger(IdeRepository::class.java)

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
      //Jsoup sets the connection timeouts itself
      val document = Jsoup.connect(repoUrl).get()
      return parseDocument(document, snapshots)
    } catch (e: Exception) {
      LOG.error("Unable to fetch repository $repoUrl index", e)
      throw e
    }
  }

  fun downloadIde(availableIde: AvailableIde,
                  saveTo: File,
                  progress: ((Double) -> Unit)? = null): File {
    if (saveTo.exists()) {
      try {
        FileUtils.forceDelete(saveTo)
      } catch (e: Exception) {
        throw IOException("Unable to delete the existing file to which the IDE should be saved", e)
      }
    }

    val call = repository.downloadIde(availableIde.downloadUrl.toExternalForm())

    val fileSize: Long
    val stream: InputStream
    try {
      val response: Response<ResponseBody> = call.executeSuccessfully()
      fileSize = response.body().contentLength()
      stream = response.body().byteStream()
    } catch (e: Exception) {
      saveTo.deleteLogged()
      LOG.error("Unable to download IDE by ${availableIde.downloadUrl}", e)
      throw e
    }

    try {
      stream.use { inputStream ->
        copyInputStreamWithProgress(inputStream, fileSize, saveTo, progress)
      }

      return saveTo
    } catch (e: Exception) {
      saveTo.deleteLogged()
      LOG.error("Unable to save the IDE #${availableIde.version} (snapshot = ${availableIde.isSnapshot}) (community = ${availableIde.isCommunity})", e)
      throw e
    }
  }

  private fun copyInputStreamWithProgress(inputStream: InputStream, fileSize: Long, toFile: File, progress: ((Double) -> Unit)?) {
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
          if (progress != null) {
            progress(count.toDouble() / fileSize)
          }
        }
      }
    }
  }

  private val repository: IdeRepositoryApi = Retrofit.Builder()
      .baseUrl(RepositoryConfiguration.ideRepositoryUrl.trimEnd('/') + '/')
      .addConverterFactory(GsonConverterFactory.create(Gson()))
      .client(makeClient())
      .build()
      .create(IdeRepositoryApi::class.java)

  private fun makeClient(): OkHttpClient = OkHttpClient.Builder()
      .connectTimeout(1, TimeUnit.HOURS)
      .readTimeout(1, TimeUnit.HOURS)
      .writeTimeout(1, TimeUnit.HOURS)
      .addInterceptor(HttpLoggingInterceptor().setLevel(if (LOG.isDebugEnabled) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE))
      .build()

}

private interface IdeRepositoryApi {

  @GET
  @Streaming
  fun downloadIde(@Url downloadUrl: String): Call<ResponseBody>

}