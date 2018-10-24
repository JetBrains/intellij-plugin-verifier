package org.jetbrains.ide.diff.builder.api

import com.jetbrains.pluginverifier.misc.extension
import com.jetbrains.pluginverifier.misc.isDirectory
import com.jetbrains.pluginverifier.misc.readText
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.cleanup.fileSize
import org.jetbrains.ide.diff.builder.BaseOldNewIdesTest
import org.jetbrains.ide.diff.builder.persistence.SinceApiReader
import org.jetbrains.ide.diff.builder.persistence.SinceApiWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path
import java.util.zip.ZipFile

class SinceApiSerializationTest : BaseOldNewIdesTest() {

  @Rule
  @JvmField
  val tempFolder = TemporaryFolder()

  private lateinit var sinceApiData: SinceApiData

  @Before
  fun build() {
    sinceApiData = SinceApiBuilderTest.buildSinceApi()
  }

  @Test
  fun `build, save and read new API data as directory`() {
    val root = tempFolder.newFolder().toPath()
    saveAndRead(sinceApiData, root)
    assertTrue(root.isDirectory)
    val annotationsXml = root.resolve("added/annotations.xml")
    assertTrue(annotationsXml.fileSize > SpaceAmount.ZERO_SPACE)
    val text = annotationsXml.readText()
    assertTrue("val=\"&quot;2.0&quot;\"" in text)
  }

  @Test
  fun `build, save and read new API data as zip`() {
    val root = tempFolder.newFile("since.zip").toPath()
    saveAndRead(sinceApiData, root)
    assertEquals("zip", root.extension)
    ZipFile(root.toFile()).use { zipFile ->
      assertTrue(zipFile.entries().asSequence().count() > 0)
      val entry = zipFile.getEntry("added/annotations.xml")
      assertTrue(entry.size > 0)
    }
  }

  private fun saveAndRead(sinceApiData: SinceApiData, root: Path) {
    SinceApiWriter(root).use {
      it.appendSinceApiData(sinceApiData)
    }

    val readSinceApiData = SinceApiReader(root).use {
      it.readSinceApiData()
    }

    assertEquals(sinceApiData.versionToApiData.keys, readSinceApiData.versionToApiData.keys)

    val apiData = sinceApiData.versionToApiData.values.first()
    val readApiData = readSinceApiData.versionToApiData.values.first()

    assertEquals(apiData.apiSignatures, readApiData.apiSignatures)
  }

}