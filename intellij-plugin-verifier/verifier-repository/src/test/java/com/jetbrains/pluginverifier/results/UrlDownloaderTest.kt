package com.jetbrains.pluginverifier.results

import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.pluginverifier.network.jsonMediaTypeValue
import com.jetbrains.pluginverifier.repository.downloader.DownloadResult
import com.jetbrains.pluginverifier.repository.downloader.UrlDownloader
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile

class UrlDownloaderTest {

  @Rule
  @JvmField
  val tempFolder = TemporaryFolder()

  @Test
  fun `downloading of a local file must copy this file`() {
    val tempDirectory = tempFolder.newFolder().toPath()
    val someFile = tempFolder.newFile("some").toPath()
    someFile.writeText("42")
    val copiedFile = assertSourceIsCopied(someFile, tempDirectory)
    assertEquals("42", copiedFile.readText())
  }

  /**
   * Copying the following directory layout:
   * some-dir/
   *    inner-dir/
   *       some.txt
   */
  @Test
  fun `downloading of a local directory must copy this directory`() {
    val tempDirectory = tempFolder.newFolder().toPath()
    val someDir = tempFolder.newFolder("some-dir").toPath()
    val innerDir = someDir.resolve("inner-dir").createDir()
    innerDir.resolve("some.txt").apply { writeText("42") }
    val copiedDirectory = assertSourceIsCopied(someDir, tempDirectory)
    val copiedSomeFile = copiedDirectory.resolve("inner-dir").resolve("some.txt")
    Assert.assertTrue(copiedSomeFile.exists())
    assertEquals("42", copiedSomeFile.readText())
  }

  /**
   * Asserts that the [original] is fully copied to a file or directory under
   * the [tempDirectory].
   * It ensures that the original file will be not be corrupted
   * when it gets returned to the caller of the [UrlDownloader].
   */
  private fun assertSourceIsCopied(original: Path, tempDirectory: Path): Path {
    val urlDownloader = UrlDownloader<Int> { original.toUri().toURL() }
    val downloadResult = urlDownloader.download(0, tempDirectory) as DownloadResult.Downloaded
    val downloadedFileOrDirectory = downloadResult.downloadedFileOrDirectory
    Assert.assertNotEquals(downloadedFileOrDirectory, original)
    assertEquals(original.nameWithoutExtension, downloadedFileOrDirectory.nameWithoutExtension)
    assertEquals(original.isDirectory, downloadedFileOrDirectory.isDirectory)
    assertEquals(original.extension, downloadedFileOrDirectory.extension)
    return downloadedFileOrDirectory
  }

  @Test
  fun `downloading a remote JSON file must succeed`() {
    val tempDirectory = tempFolder.newFolder().toPath()

    MockWebServer().use { server ->
      val jsonContent = "\"some-plain-text\""
      val response = MockResponse().setBody(jsonContent).setHeader("Content-Type", jsonMediaTypeValue)
      server.enqueue(response)
      server.start()
      val downloader = UrlDownloader<Int> {
        server.url("/").toUrl()
      }
      val downloadResult = downloader.download(0, tempDirectory) as DownloadResult.Downloaded
      val downloadedFileOrDirectory = downloadResult.downloadedFileOrDirectory
      assertTrue(downloadedFileOrDirectory.isRegularFile())
      assertEquals("json", downloadedFileOrDirectory.extension)
      assertEquals(jsonContent, downloadedFileOrDirectory.readText())
    }
  }

  @Test
  fun `downloading a file with json extension must correctly guess such extension`() {
    val tempDirectory = tempFolder.newFolder().toPath()

    MockWebServer().use { server ->
      val jsonContent = "\"some-plain-text\""
      val response = MockResponse().setBody(jsonContent)
      server.enqueue(response)
      server.start()
      val downloader = UrlDownloader<Int> {
        server.url("/data.json").toUrl()
      }
      val downloadResult = downloader.download(0, tempDirectory) as DownloadResult.Downloaded
      val downloadedFileOrDirectory = downloadResult.downloadedFileOrDirectory
      assertTrue(downloadedFileOrDirectory.isRegularFile())
      assertEquals("json", downloadedFileOrDirectory.extension)
      assertEquals(jsonContent, downloadedFileOrDirectory.readText())
    }
  }

  @Test
  fun `downloading a file with Content Disposition as txt must correctly guess such extension`() {
    val tempDirectory = tempFolder.newFolder().toPath()

    MockWebServer().use { server ->
      val jsonContent = "\"some-plain-text\""
      val response = MockResponse()
              .setBody(jsonContent)
              .setHeader("Content-Disposition", "form-data; name=\"fieldName\"; filename=\"filename.txt\"")
      server.enqueue(response)
      server.start()
      val downloader = UrlDownloader<Int> {
        server.url("/items/1").toUrl()
      }
      val downloadResult = downloader.download(0, tempDirectory) as DownloadResult.Downloaded
      val downloadedFileOrDirectory = downloadResult.downloadedFileOrDirectory
      assertTrue(downloadedFileOrDirectory.isRegularFile())
      assertEquals("txt", downloadedFileOrDirectory.extension)
      assertEquals(jsonContent, downloadedFileOrDirectory.readText())
    }
  }
}