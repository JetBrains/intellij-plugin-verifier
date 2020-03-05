package com.jetbrains.plugin.structure.zipBombs

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.decompress.DecompressorSizeLimitExceededException
import com.jetbrains.plugin.structure.base.utils.extractTo
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.random.Random

class DecompressorSizeLimitTest {
  companion object {
    fun generateZipFileOfSizeAtLeast(zipFile: File, size: Long): File {
      val random = Random(42)
      val zipBomb = buildZipFile(zipFile) {
        file("bomb.bin", random.nextBytes((size * 100).toInt()))
      }
      Assert.assertTrue(zipBomb.length() > size)
      return zipBomb
    }
  }

  @Rule
  @JvmField
  val tempFolder = TemporaryFolder()

  @Test(expected = DecompressorSizeLimitExceededException::class)
  fun `limit is set`() {
    val tempDirectory = tempFolder.newFolder()
    val zipFile = generateZipFileOfSizeAtLeast(tempDirectory.resolve("big.zip"), 1001)
    zipFile.extractTo(tempDirectory.resolve("extracted"), 1000)
  }
}