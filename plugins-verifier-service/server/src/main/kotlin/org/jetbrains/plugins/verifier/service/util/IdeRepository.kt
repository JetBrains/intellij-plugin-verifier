package org.jetbrains.plugins.verifier.service.util

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.gson.annotations.SerializedName
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.persistence.GsonHolder
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.jetbrains.plugins.verifier.service.core.Progress
import org.jetbrains.plugins.verifier.service.core.TaskManager
import org.jetbrains.plugins.verifier.service.runners.UploadIdeRunner
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.storage.FileManager
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Patrikeev
 */
object IdeRepository {

  private val LOG: Logger = LoggerFactory.getLogger(IdeRepository::class.java)

  //30 minutes
  private val DOWNLOAD_NEW_IDE_PERIOD: Long = 30

  fun run() {
    Executors.newSingleThreadScheduledExecutor(
        ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("ide-repository-%d")
            .build()
    ).scheduleAtFixedRate({ IdeRepository.tick() }, 0, DOWNLOAD_NEW_IDE_PERIOD, TimeUnit.MINUTES)
  }

  private fun tick() {
    LOG.info("It's time to upload new IDE versions to the verifier service")
    val alreadyIdes = IdeFilesManager.ideList()
    val maxVersion = alreadyIdes.max()

    fetchIndex(false).artifacts
        .filter { it.artifactId == "ideaIU" }
        .filter { it.packaging == "zip" }
        .map { IdeVersion.createIdeVersion(it.version) }
        .filter { maxVersion == null || maxVersion < it }
        .forEach {
          val runner = UploadIdeRunner(it.asString(), false, false)

          val taskId = TaskManager.enqueue(runner)
          LOG.info("Uploading IDE version #$it is enqueued with taskId = #$taskId")
        }
  }

  fun fetchIndex(fromSnapshots: Boolean = false): RepositoryIndex {
    val call = repository.fetchIndex(getRepo(fromSnapshots))
    try {
      return call.executeSuccessfully().body()
    } catch (e: Exception) {
      LOG.error("Unable to fetch repository ${call.request().url()} index", e)
      throw e
    }
  }

  fun download(ideVersion: String, progress: Progress, isCommunity: Boolean = false, fromSnapshots: Boolean = false): File {
    val ideaName = if (isCommunity) "ideaIC" else "ideaIU"
    val call = repository.downloadFrom(getRepo(fromSnapshots), ideaName, ideVersion)
    val body: ResponseBody
    try {
      body = call.executeSuccessfully().body()
    } catch (e: Exception) {
      LOG.error("Unable to download #$ideVersion (snapshot = $fromSnapshots) (community = $isCommunity)", e)
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

  private fun getRepo(fromSnapshots: Boolean) = if (fromSnapshots) "snapshots" else "releases"

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
      .baseUrl(Settings.IDE_REPOSITORY_URL.get().trimEnd('/') + '/')
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

data class RepositoryIndex(@SerializedName("artifacts") val artifacts: List<ArtifactIndex>)

data class ArtifactIndex(@SerializedName("groupId") val groupId: String,
                         @SerializedName("artifactId") val artifactId: String,
                         @SerializedName("version") val version: String,
                         @SerializedName("classifier") val classifier: String,
                         @SerializedName("packaging") val packaging: String,
                         @SerializedName("lastModified") val lastModified: String,
                         @SerializedName("lastModifiedUnixTimeMs") val lastModifiedUnixTimeMs: Long,
                         @SerializedName("eTag") val eTag: String)


interface RepositoryInterface {

  @Streaming
  @GET("/intellij-repository/{repo}/com/jetbrains/intellij/idea/{ideaName}/{ideVersion}/{ideaName}-{ideVersion}.zip")
  fun downloadFrom(@Path("repo") repo: String, @Path("ideaName") ideaName: String, @Path("ideVersion") ideVersion: String): Call<ResponseBody>

  @GET("/intellij-repository/{repo}/index.json")
  fun fetchIndex(@Path("repo") repo: String): Call<RepositoryIndex>

}