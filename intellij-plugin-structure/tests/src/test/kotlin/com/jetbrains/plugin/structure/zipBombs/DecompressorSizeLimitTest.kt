package com.jetbrains.plugin.structure.zipBombs

import com.jetbrains.plugin.structure.base.decompress.DecompressorSizeLimitExceededException
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.extractTo
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.random.Random

class DecompressorSizeLimitTest {
  companion object {
    fun generateZipFileOfSizeAtLeast(zipFile: Path, size: Long): Path {
      val random = Random(42)
      val zipBomb = buildZipFile(zipFile) {
        file("bomb.bin", random.nextBytes((size * 100).toInt()))
      }
      Assert.assertTrue(Files.size(zipBomb) > size)
      return zipBomb
    }
  }

  @Rule
  @JvmField
  val tempFolder = TemporaryFolder()

  @Test(expected = DecompressorSizeLimitExceededException::class)
  fun `limit is set`() {
    val tempDirectory = tempFolder.newFolder()
    val zipFile = generateZipFileOfSizeAtLeast(tempDirectory.resolve("big.zip").toPath(), 1001)
    zipFile.extractTo(tempDirectory.toPath().resolve("extracted"), 1000)
  }
}