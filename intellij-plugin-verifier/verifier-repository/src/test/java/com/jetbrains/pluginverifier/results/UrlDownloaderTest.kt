package com.jetbrains.pluginverifier.results

import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.pluginverifier.repository.downloader.DownloadResult
import com.jetbrains.pluginverifier.repository.downloader.UrlDownloader
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

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
}