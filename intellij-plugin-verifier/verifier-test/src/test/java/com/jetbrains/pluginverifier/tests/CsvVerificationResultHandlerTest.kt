package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import com.jetbrains.plugin.structure.ide.PluginIdAndVersion
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.telemetry.CsvVerificationResultHandler
import com.jetbrains.pluginverifier.telemetry.VerificationSpecificTelemetry
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.nio.file.Path
import kotlin.io.path.createTempFile
import kotlin.io.path.readText

class CsvVerificationResultHandlerTest {
  private lateinit var verificationResultHandler: CsvVerificationResultHandler

  private lateinit var csvOutputPath: Path

  @Before
  fun setUp() {
    csvOutputPath = createTempFile()
    verificationResultHandler = CsvVerificationResultHandler(csvOutputPath)
  }

  @Test
  fun `empty telemetry is written with missing or default values`() {
    val ideVersion = IdeVersion.createIdeVersion("233.0")
    val pluginIdAndVersion = PluginIdAndVersion("somePlugin", "1.0")
    val telemetry = VerificationSpecificTelemetry(ideVersion, pluginIdAndVersion, PluginTelemetry())
    verificationResultHandler.use {
      it.beforeVerificationResult()
      it.onVerificationResult(telemetry)
      it.afterVerificationResult()
    }

    val generatedCsv = csvOutputPath.readText()
    val expectedCsv = """
      IDE version,Plugin ID,Plugin Version,Plugin Size,Verification Time,Description Parsing Time
      $ideVersion,${pluginIdAndVersion.pluginId},${pluginIdAndVersion.version},-1,,
      
    """.trimIndent()
    Assert.assertEquals(expectedCsv, generatedCsv)
  }
}