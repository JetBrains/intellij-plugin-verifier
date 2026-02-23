/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.downloader

import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.pluginverifier.misc.createHttpClient
import com.jetbrains.pluginverifier.network.*
import com.jetbrains.pluginverifier.network.HttpHeaders.CONTENT_DISPOSITION
import com.jetbrains.pluginverifier.network.HttpHeaders.CONTENT_LENGTH
import com.jetbrains.pluginverifier.network.HttpHeaders.CONTENT_TYPE
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration

private const val FILENAME = "filename="
private val urlPathExtensions = listOf("jar", "zip", "tar.gz", "tar.bz2", "txt", "html", "xml", "json")

/**
 * [Downloader] of files for URLs provided with [urlProvider].
 */
class UrlDownloader<in K>(private val urlProvider: (K) -> URL?) : Downloader<K> {

  private companion object {
    private const val FILE_PROTOCOL = "file"
    private const val HTTP_PROTOCOL = "http"
    private const val HTTPS_PROTOCOL = "https"

    private val LOG = LoggerFactory.getLogger(UrlDownloader::class.java)

  }

  private val downloadConnector = DownloadConnector()

  @Throws(InterruptedException::class)
  override fun download(key: K, tempDirectory: Path): DownloadResult {
    val downloadUrl = try {
      urlProvider(key)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
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
      e.rethrowIfInterrupted()
      DownloadResult.FailedToDownload("Unable to download $key: ${e.message}", e)
    }
  }

  private fun doDownload(key: K, downloadUrl: URL, tempDirectory: Path): DownloadResult {
    return when (val protocol = downloadUrl.protocol) {
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
    val urlString = downloadUrl.toExternalForm()
    val event = PluginDownloadEvent(urlString)
    event.begin()
    try {
      val response = downloadConnector.download(urlString)
      event.contentLength = response.contentLength
      event.extension = response.extension
      val extension = response.extension
      val downloadedTempFile = Files.createTempFile(tempDirectory, "", ".$extension")
      return try {
        LOG.debug("Downloading {} to {}", key, downloadedTempFile)
        copyResponseTo(response, downloadedTempFile)
        DownloadResult.Downloaded(downloadedTempFile, extension, false)
      } catch (e: Throwable) {
        downloadedTempFile.deleteLogged()
        throw e
      }
    } finally {
      event.commit()
    }
  }

  private fun copyResponseTo(response: Response, file: Path) {
    checkIfInterrupted()
    response.body.use { responseBody ->
      Files.copy(responseBody, file, StandardCopyOption.REPLACE_EXISTING)
    }
  }

  private data class Response(val body: InputStream, val contentLength: Long = -1, val extension: String)

  private class DownloadConnector {
    val httpClient = createHttpClient()

    fun download(url: String, timeout: Duration = Duration.ofMinutes(5)): Response {
      val httpGet = HttpRequest.newBuilder().GET()
              .uri(URI.create(url))
              .timeout(timeout)
              .build()

      val response: HttpResponse<InputStream> = httpClient.send(httpGet, HttpResponse.BodyHandlers.ofInputStream())
      assertHttpOk(url, response)
      val extension = response.guessExtension()
      return Response(response.body(), response.contentLength(), extension)
    }

    private fun assertHttpOk(url: String, response: HttpResponse<InputStream>) {
      when (val code = response.statusCode()) {
        200 -> return
        404 -> throw NotFound404ResponseException(url)
        500 -> throw ServerInternalError500Exception(url)
        503 -> throw ServerUnavailable503Exception(url)
        else -> {
          val message = response.body().bufferedReader().readLine().take(255)
          throw NonSuccessfulResponseException(url, code, message)
        }
      }
    }
  }
}

private fun HttpResponse<*>.contentLength(): Long {
  return this.headers()
          .firstValueAsLong(CONTENT_LENGTH)
          .orElse(-1L)
}

internal fun HttpResponse<*>.guessExtension(defaultExtension: String  = "zip"): String {
  /**
   * Guess by Content-Disposition header.
   */
  val contentDisposition: String? = headers().firstValue(CONTENT_DISPOSITION).orElse(null)

  if (contentDisposition != null && contentDisposition.contains(FILENAME)) {
    val path = contentDisposition.substringAfter(FILENAME).substringBefore(";").removeSurrounding("\"")
    val extension = guessExtensionByPath(path)
    if (extension != null) {
      return extension
    }
  }

  /**
   * Guess by content type.
   */
  val contentType = headers().firstValue(CONTENT_TYPE).orElse(octetStreamMediaTypeValue)
  if (contentType == jarContentMediaTypeValue || contentType == xJarContentMediaTypeValue) {
    return "jar"
  }
  if (contentType == jsonMediaTypeValue) {
    return "json"
  }

  /**
   * Guess by URL path extension.
   */
  val path = this.request().uri().rawPath
  val extension = guessExtensionByPath(path)
  if (extension != null) {
    return extension
  }

  return defaultExtension
}

private fun guessExtensionByPath(path: String): String? {
  return urlPathExtensions.firstOrNull { path.endsWith(".$it") }
}