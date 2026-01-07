/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.reporting

import com.jetbrains.plugin.structure.base.problems.InvalidPluginIDProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.jdk.JdkVersion
import com.jetbrains.pluginverifier.reporting.common.LogReporter
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.tests.mocks.MockLogger
import com.jetbrains.pluginverifier.warnings.PluginStructureError
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.io.path.createTempDirectory

private const val PLUGIN_ID = "pluginId"
private const val PLUGIN_VERSION = "1.0"
private const val ANOTHER_PLUGIN_ID = "anotherPluginId"
private const val ANOTHER_PLUGIN_VERSION = "2.0"

class LoggingPluginVerificationReportageAggregatorTest {
  private lateinit var loggingPluginVerificationReportageAggregator: LoggingPluginVerificationReportageAggregator
  private lateinit var mockLogger: MockLogger

  private val pluginInfo = mockPluginInfo()
  private val verificationTarget = PluginVerificationTarget.IDE(IdeVersion.createIdeVersion("232"), JdkVersion("11", null))

  private val anotherPluginInfo = anotherMockPluginInfo()
  private val anotherVerificationTarget = PluginVerificationTarget.IDE(IdeVersion.createIdeVersion("231"), JdkVersion("11", null))

  @Before
  fun setUp() {
    mockLogger = MockLogger()
    val logReporters = listOf(LogReporter<String>(mockLogger))
    loggingPluginVerificationReportageAggregator = LoggingPluginVerificationReportageAggregator(logReporters)
  }

  @Test
  fun `verification results are aggregated`() {
    val dependenciesGraph = DependenciesGraph(
      verifiedPlugin = DependencyNode(PLUGIN_ID, PLUGIN_VERSION),
      vertices = emptySet(),
      edges = emptySet(),
      missingDependencies = emptyMap()
    )
    val verificationResult = PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph)
    val targetDirectory = createTempDirectory()
    loggingPluginVerificationReportageAggregator.handleVerificationResult(verificationResult, targetDirectory)
    loggingPluginVerificationReportageAggregator.handleAggregatedReportage()

    assertEquals(1, mockLogger.loggingEvents.size)
    val logEntry = mockLogger.loggingEvents.first()
    assertEquals("Verification reports for $PLUGIN_ID $PLUGIN_VERSION saved to $targetDirectory", logEntry.messagePattern)
  }

  @Test
  fun `verification results are aggregated on failed verification`() {
    val pluginStructureErrors = setOf(PluginStructureError(InvalidPluginIDProblem(IdePluginManager.PLUGIN_XML)))
    val verificationResult = PluginVerificationResult.InvalidPlugin(pluginInfo, verificationTarget, pluginStructureErrors)
    val targetDirectory = createTempDirectory()
    loggingPluginVerificationReportageAggregator.handleVerificationResult(verificationResult, targetDirectory)
    loggingPluginVerificationReportageAggregator.handleAggregatedReportage()

    assertEquals(1, mockLogger.loggingEvents.size)
    val logEntry = mockLogger.loggingEvents.first()
    assertEquals("Verification reports for $PLUGIN_ID $PLUGIN_VERSION saved to $targetDirectory", logEntry.messagePattern)
  }

  @Test
  fun `verification results are emitted from multiple plugins`() {
    val dependenciesGraph = DependenciesGraph(
      verifiedPlugin = DependencyNode(PLUGIN_ID, PLUGIN_VERSION),
      vertices = emptySet(),
      edges = emptySet(),
      missingDependencies = emptyMap()
    )
    val verificationResult = PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph)
    val targetDirectory = createTempDirectory()

    val anotherDependenciesGraph = DependenciesGraph(
      verifiedPlugin = DependencyNode(ANOTHER_PLUGIN_ID, ANOTHER_PLUGIN_VERSION),
      vertices = emptySet(),
      edges = emptySet(),
      missingDependencies = emptyMap()
    )
    val anotherVerificationResult = PluginVerificationResult.Verified(anotherPluginInfo, anotherVerificationTarget, anotherDependenciesGraph)
    val anotherTargetDirectory = createTempDirectory()

    // run the verifications for the first plugin
    loggingPluginVerificationReportageAggregator.handleVerificationResult(verificationResult, targetDirectory)

    // run the verifications for another plugin
    loggingPluginVerificationReportageAggregator.handleVerificationResult(anotherVerificationResult, anotherTargetDirectory)

    // aggregate the reportages for both plugins
    loggingPluginVerificationReportageAggregator.handleAggregatedReportage()

    assertEquals(2, mockLogger.loggingEvents.size)
    val logEntry = mockLogger.loggingEvents[0]
    assertEquals("Verification reports for $PLUGIN_ID $PLUGIN_VERSION saved to $targetDirectory", logEntry.messagePattern)

    val anotherLogEntry = mockLogger.loggingEvents[1]
    assertEquals("Verification reports for $ANOTHER_PLUGIN_ID $ANOTHER_PLUGIN_VERSION saved to $anotherTargetDirectory", anotherLogEntry.messagePattern)
  }


  private fun mockPluginInfo(): PluginInfo =
    object : PluginInfo(PLUGIN_ID, PLUGIN_ID, PLUGIN_VERSION, null, null, null) {}

  private fun anotherMockPluginInfo(): PluginInfo =
    object : PluginInfo(ANOTHER_PLUGIN_ID, ANOTHER_PLUGIN_ID, ANOTHER_PLUGIN_VERSION, null, null, null) {}

}