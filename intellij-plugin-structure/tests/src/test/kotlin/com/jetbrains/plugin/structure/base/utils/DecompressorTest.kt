package com.jetbrains.plugin.structure.base.utils

import com.jetbrains.plugin.structure.base.decompress.EntryNameTooLongException
import com.jetbrains.plugin.structure.base.decompress.InvalidRelativeEntryNameException
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
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
}