package org.jetbrains.ide.diff.builder.api

import com.jetbrains.pluginverifier.misc.extension
import com.jetbrains.pluginverifier.misc.isDirectory
import com.jetbrains.pluginverifier.misc.readText
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.cleanup.fileSize
import org.jetbrains.ide.diff.builder.BaseOldNewIdesTest
import org.jetbrains.ide.diff.builder.persistence.ApiReportReader
import org.jetbrains.ide.diff.builder.persistence.ApiReportWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path
import java.util.zip.ZipFile

class ApiReportSerializationTest : BaseOldNewIdesTest() {

  @Rule
  @JvmField
  val tempFolder = TemporaryFolder()

  private lateinit var apiReport: ApiReport

  @Before
  fun build() {
    apiReport = IdeDiffBuilderTest().buildApiReport()
  }

  @Test
  fun `build, save and read API report as directory`() {
    val root = tempFolder.newFolder().toPath()
    saveAndRead(apiReport, root)
    assertTrue(root.isDirectory)
    val annotationsXml = root.resolve("added/annotations.xml")
    assertTrue(annotationsXml.fileSize > SpaceAmount.ZERO_SPACE)
    val text = annotationsXml.readText()
    assertTrue("val=\"&quot;2.0&quot;\"" in text)
  }

  @Test
  fun `build, save and read API report as zip`() {
    val root = tempFolder.newFile("report.zip").toPath()
    saveAndRead(apiReport, root)
    assertEquals("zip", root.extension)
    ZipFile(root.toFile()).use { zipFile ->
      assertTrue(zipFile.entries().asSequence().count() > 0)
      val entry = zipFile.getEntry("added/annotations.xml")
      assertTrue(entry.size > 0)
    }
  }

  private fun saveAndRead(apiReport: ApiReport, root: Path) {
    ApiReportWriter(root, apiReport.ideBuildNumber).use {
      it.appendApiReport(apiReport)
    }

    val readApiReport = ApiReportReader(root).use {
      it.readApiReport()
    }

    assertEquals(apiReport.apiEventToData.keys, readApiReport.apiEventToData.keys)

    val apiData = apiReport.apiEventToData.values.first()
    val readApiData = readApiReport.apiEventToData.values.first()

    assertEquals(apiData.apiSignatures, readApiData.apiSignatures)
  }

}