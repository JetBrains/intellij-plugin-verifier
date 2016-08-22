package com.jetbrains.pluginverifier.misc

import org.apache.commons.io.FileUtils
import org.apache.http.annotation.ThreadSafe
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author Sergey Evdokimov
 */
@ThreadSafe
object DownloadManager {

  @Throws(IOException::class)
  private fun getOrCreateDownloadDir(): File {
    val downloadDir = RepositoryConfiguration.pluginCacheDir
    if (!downloadDir.isDirectory) {
      FileUtils.forceMkdir(downloadDir)
      if (!downloadDir.isDirectory) {
        throw IOException("Failed to create temp directory: " + downloadDir)
      }
    }

    return downloadDir
  }

  private fun getCacheFileName(updateId: Int): String = "$updateId.zip"

  /**
   * Performs necessary redirection
   */
  @Throws(IOException::class)
  private fun getFinalUrl(url: URL): URL {
    val connection = url.openConnection() as HttpURLConnection
    connection.instanceFollowRedirects = false

    if (connection.responseCode == HttpURLConnection.HTTP_ACCEPTED) {
      return url
    }

    if (connection.responseCode == HttpURLConnection.HTTP_MOVED_TEMP || connection.responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
      val location = connection.getHeaderField("Location")
      if (location != null) {
        return URL(location)
      }
    }
    return url
  }

  @Throws(MalformedURLException::class)
  private fun getUrlForUpdate(updateId: Int): URL {
    return URL(RepositoryConfiguration.pluginRepositoryUrl + "/plugin/download/?noStatistic=true&updateId=" + updateId)
  }

  @Throws(IOException::class)
  fun getOrLoadUpdate(updateId: Int): File? {
    val downloadDir = getOrCreateDownloadDir()

    var url = getUrlForUpdate(updateId)
    try {
      url = getFinalUrl(url)
    } catch (e: IOException) {
      throw IOException("The repository " + url.host + " problems", e)
    }

    val pluginInCache = File(downloadDir, getCacheFileName(updateId))

    if (!pluginInCache.exists() || pluginInCache.length() < BROKEN_ZIP_THRESHOLD) {
      val currentDownload = File.createTempFile("currentDownload", ".zip", downloadDir)

      LOG.debug("Downloading {} by url {}... ", updateId, url)

      var downloadFail = true
      try {
        FileUtils.copyURLToFile(url, currentDownload)

        if (currentDownload.length() < BROKEN_ZIP_THRESHOLD) {
          LOG.error("Broken zip archive by url {} of file {}", url, currentDownload)
          return null
        }

        LOG.debug("Plugin {} is downloaded", updateId)
        downloadFail = false
      } catch (e: Exception) {
        LOG.error("Error loading plugin " + updateId + " by " + url.toExternalForm(), e)
        return null
      } finally {
        if (downloadFail) {
          deleteLogged(currentDownload)
        }
      }

      //provides the thread safety while multiple threads attempt to load the same plugin.
      synchronized(this) {
        try {
          if (pluginInCache.exists()) {
            //remove the old (possibly broken plugin)
            FileUtils.forceDelete(pluginInCache)
          }
        } catch (e: Exception) {
          LOG.error("Unable to delete cached plugin file " + pluginInCache, e)
          deleteLogged(currentDownload)
          throw e
        }

        try {
          FileUtils.moveFile(currentDownload, pluginInCache)
        } catch (e: Exception) {
          LOG.error("Unable to move downloaded plugin file $currentDownload to $pluginInCache", e)
          deleteLogged(currentDownload)
          deleteLogged(pluginInCache)
          throw e
        }

      }

    }

    return pluginInCache
  }

  private fun deleteLogged(file: File) {
    if (file.exists()) {
      try {
        FileUtils.forceDelete(file)
      } catch (ce: Exception) {
        LOG.error("Unable to delete file " + file, ce)
      }

    }
  }

  private val LOG = LoggerFactory.getLogger(DownloadManager::class.java)

  private val BROKEN_ZIP_THRESHOLD = 200
  private val httpDateFormat: DateFormat

  init {
    httpDateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
    httpDateFormat.timeZone = TimeZone.getTimeZone("GMT")
  }

}
