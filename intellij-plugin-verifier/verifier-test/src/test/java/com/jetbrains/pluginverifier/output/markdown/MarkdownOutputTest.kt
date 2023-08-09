package com.jetbrains.pluginverifier.output.markdown

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.jdk.JdkVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.PrintWriter
import java.io.StringWriter

private const val PLUGIN_ID = "pluginId"
private const val PLUGIN_VERSION = "1.0"

class MarkdownOutputTest {
  private val pluginInfo = mockPluginInfo(PLUGIN_ID, PLUGIN_VERSION)
  private val verificationTarget = PluginVerificationTarget.IDE(IdeVersion.createIdeVersion("232"), JdkVersion("11", null))

  @Test
  fun `plugin is compatible`() {
    val dependenciesGraph = DependenciesGraph(
      verifiedPlugin = DependencyNode(PLUGIN_ID, PLUGIN_VERSION),
      vertices = emptyList(),
      edges = emptyList(),
      missingDependencies = emptyMap()
      )
    val verificationResult = PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph)

    val out = StringWriter()
    val resultPrinter = MarkdownResultPrinter(PrintWriter(out))
    resultPrinter.printResults(listOf(verificationResult))

    val expected = """
      # Plugin pluginId $PLUGIN_VERSION against 232.0: Compatible
      
      
      """.trimIndent()
    assertEquals(expected, out.buffer.toString())
  }
}

fun mockPluginInfo(pluginId: String, version: String): PluginInfo =
  object : PluginInfo(pluginId, pluginId, version, null, null, null) {}
