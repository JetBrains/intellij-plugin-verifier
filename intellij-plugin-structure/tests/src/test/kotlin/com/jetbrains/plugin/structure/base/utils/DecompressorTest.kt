package com.jetbrains.plugin.structure.base.utils

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DecompressorTest {

  @Rule
  @JvmField
  val tempFolder = TemporaryFolder()

  @Rule
  @JvmField
  val expectedEx: ExpectedException = ExpectedException.none()

  @Test
  fun simple() {
    val zipFile = buildZipFile(tempFolder.newFile("some.zip")) {
      file("some.txt", "content")
    }
    val destination = tempFolder.newFolder()
    zipFile.extractTo(destination)
    val someTxt = destination.resolve("some.txt")
    assertEquals(listOf(someTxt), destination.listFiles().orEmpty().toList())
    assertEquals("content", someTxt.readText())
  }

  @Test
  fun `contains file with relative path`() {
    val relativeName = "some/../relative.txt"

    expectedEx.expect(IOException::class.java)
    expectedEx.expectMessage("Invalid relative entry name: some/../relative.txt")

    val zipFile = tempFolder.newFile("broken.zip")
    ZipOutputStream(zipFile.outputStream()).use {
      it.putNextEntry(ZipEntry(relativeName))
      it.write("42".toByteArray())
      it.closeEntry()
    }

    zipFile.extractTo(tempFolder.newFolder())
  }

  @Test
  fun `contains file with too long name`() {
    val tooLongName = "a".repeat(256)

    expectedEx.expect(IOException::class.java)
    expectedEx.expectMessage("Entry name is too long: $tooLongName")

    val zipFile = tempFolder.newFile("broken.zip")
    ZipOutputStream(zipFile.outputStream()).use {
      it.putNextEntry(ZipEntry(tooLongName))
      it.write("42".toByteArray())
      it.closeEntry()
    }

    zipFile.extractTo(tempFolder.newFolder())
  }
}