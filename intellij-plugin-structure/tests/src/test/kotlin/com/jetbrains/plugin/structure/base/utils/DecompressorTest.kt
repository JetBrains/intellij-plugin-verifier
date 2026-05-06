package com.jetbrains.plugin.structure.base.utils

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.jetbrains.plugin.structure.base.decompress.DuplicateZipEntryException
import com.jetbrains.plugin.structure.base.decompress.EntryNameTooLongException
import com.jetbrains.plugin.structure.base.decompress.InvalidRelativeEntryNameException
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DecompressorTest {

  @Rule
  @JvmField
  val tempFolder = TemporaryFolder()

  @Test
  fun simple() {
    val zipFile = buildZipFile(tempFolder.newFile("some.zip").toPath()) {
      file("some.txt", "content")
    }
    val destination = tempFolder.newFolder()
    zipFile.extractTo(destination.toPath())
    val someTxt = destination.resolve("some.txt")
    assertEquals(listOf(someTxt), destination.listFiles().orEmpty().toList())
    assertEquals("content", someTxt.readText())
  }

  @Test
  fun `contains file with relative path`() {
    val relativeName = "../relative.txt"

    Assert.assertThrows("Invalid relative entry name: ../relative.txt", InvalidRelativeEntryNameException::class.java) {
      val zipFile = tempFolder.newFile("broken.zip").toPath()
      ZipOutputStream(Files.newOutputStream(zipFile)).use {
        it.putNextEntry(ZipEntry(relativeName))
        it.write("42".toByteArray())
        it.closeEntry()
      }

      zipFile.extractTo(tempFolder.newFolder().toPath())
    }
  }

  @Test
  fun `contains file with too long name`() {
    val tooLongName = "a".repeat(256)

    Assert.assertThrows("Entry name is too long: $tooLongName", EntryNameTooLongException::class.java) {
      val zipFile = tempFolder.newFile("broken.zip").toPath()
      ZipOutputStream(Files.newOutputStream(zipFile)).use {
        it.putNextEntry(ZipEntry(tooLongName))
        it.write("42".toByteArray())
        it.closeEntry()
      }

      zipFile.extractTo(tempFolder.newFolder().toPath())
    }
  }

  @Test
  fun `duplicate file entry fails with DuplicateZipEntryException`() {
    val zipFile = createZipWithDuplicateEntry(
      tempFolder.newFile("duplicate.zip").toPath(),
      duplicateEntryName = "file.txt",
      firstContent = "first",
      secondContent = "second",
    )

    Assert.assertThrows(DuplicateZipEntryException::class.java) {
      zipFile.extractTo(tempFolder.newFolder().toPath())
    }
  }

  @Test
  fun `entry with Windows absolute path is rejected on Windows filesystem`() {
    val zipFile = tempFolder.newFile("path-traversal.zip").toPath()
    ZipOutputStream(Files.newOutputStream(zipFile)).use {
      it.putNextEntry(ZipEntry("C:/Users/Public/pwned.txt"))
      it.write("pwn".toByteArray())
      it.closeEntry()
    }

    Jimfs.newFileSystem(Configuration.windows()).use { windowsFs ->
      val outputDir = Files.createDirectories(windowsFs.getPath("C:\\output"))
      Assert.assertThrows(InvalidRelativeEntryNameException::class.java) {
        extractZip(zipFile, outputDir)
      }
    }
  }

  @Test
  fun `entry with Windows absolute path with backslashes is rejected on Windows filesystem`() {
    val zipFile = tempFolder.newFile("path-traversal.zip").toPath()
    ZipOutputStream(Files.newOutputStream(zipFile)).use {
      it.putNextEntry(ZipEntry("C:\\Users\\Public\\pwned.txt"))
      it.write("pwn".toByteArray())
      it.closeEntry()
    }

    Jimfs.newFileSystem(Configuration.windows()).use { windowsFs ->
      val outputDir = Files.createDirectories(windowsFs.getPath("C:\\output"))
      Assert.assertThrows(InvalidRelativeEntryNameException::class.java) {
        extractZip(zipFile, outputDir)
      }
    }
  }
  @Test
  fun `empty directory is properly decompressed`() {
    val zipFile = tempFolder.newFile("empty-dir.zip").toPath()
    ZipOutputStream(Files.newOutputStream(zipFile)).use {
      it.putNextEntry(ZipEntry("dir/"))
      it.closeEntry()
    }
    val destinationPath = tempFolder.newFolder().toPath()
    zipFile.extractTo(destinationPath)
    val extractedFiles = destinationPath.listFiles()
    assertEquals(listOf(destinationPath.resolve("dir")), extractedFiles)
  }
}