/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.output

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.problems.ForbiddenPluginIdPrefix
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.dymamic.DynamicPluginStatus
import com.jetbrains.pluginverifier.jdk.JdkVersion
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.tasks.InvalidPluginFile
import org.junit.Assert.assertEquals
import java.io.StringWriter
import kotlin.io.path.Path

typealias VerifiedPluginHandler = (PluginVerificationResult.Verified) -> Unit

open class BaseOutputPrintTest<T : ResultPrinter>: BaseOutputTest() {
  private val pluginInfo = mockPluginInfo()
  private val verificationTarget = PluginVerificationTarget.IDE(IdeVersion.createIdeVersion("232"), JdkVersion("11", null))

  protected lateinit var out: StringWriter
  protected lateinit var resultPrinter: T

  fun interface VerifiedPluginWithPrinterRunner<T, R> {
    fun run(resultPrinter: T, result: R)
  }

  open fun setUp() {
    out = StringWriter()
  }

  private val dependenciesGraph: DependenciesGraph = DependenciesGraph(
        verifiedPlugin = DependencyNode.IdAndVersionDependency(PLUGIN_ID, PLUGIN_VERSION),
        vertices = emptySet(),
        edges = emptySet(),
        missingDependencies = emptyMap())

  open fun `when plugin is compatible`(testRunner: VerifiedPluginHandler) {
    testRunner.runTest(PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph))
  }

  open fun `when plugin has compatibility warnings`(testRunner: VerifiedPluginHandler) {
    testRunner.runTest(PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph, mockCompatibilityProblems()))
  }

  open fun `when plugin has structural problems`(testRunner: VerifiedPluginHandler) {
    testRunner.runTest(PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph, pluginStructureWarnings = mockStructureWarnings()))
  }

  open fun `when plugin has internal API usage problems`(testRunner: VerifiedPluginHandler) {
    testRunner.runTest(PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph, internalApiUsages = internalApiUsages()))
  }

  open fun `when plugin has non-extendable API usages problems`(testRunner: VerifiedPluginHandler) {
    testRunner.runTest(PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph, nonExtendableApiUsages = mockNonExtendableApiUsages()))
  }

  open fun `when plugin has experimental API usage problems`(testRunner: VerifiedPluginHandler) {
    testRunner.runTest(PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph, experimentalApiUsages = mockExperimentalApiUsages()))
  }

  open fun `when plugin has missing dependencies`(testRunner: VerifiedPluginHandler) {
    val pluginDependency = DependencyNode.IdAndVersionDependency(PLUGIN_ID, PLUGIN_VERSION)
    val expectedDependency = MissingDependency(PluginDependencyImpl("MissingPlugin", true, false), "Dependency MissingPlugin is not found among the bundled plugins of IU-211.500")

    val dependenciesGraph = DependenciesGraph(
      verifiedPlugin = pluginDependency,
      vertices = emptySet(),
      edges = emptySet(),
      missingDependencies = mapOf(pluginDependency to setOf(expectedDependency))
    )

    testRunner.runTest(PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph))
  }

  open fun `when plugin is dynamic`(testRunner: VerifiedPluginHandler) {
    testRunner.runTest(PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph, dynamicPluginStatus = DynamicPluginStatus.MaybeDynamic))
  }

  open fun `when plugin is dynamic and has structural warnings`(testRunner: VerifiedPluginHandler) {
    testRunner.runTest(PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph,
      dynamicPluginStatus = DynamicPluginStatus.MaybeDynamic,
      pluginStructureWarnings = mockStructureWarnings()
    ))
  }

  open fun `when plugin has structural problems with invalid plugin ID`(testRunner: VerifiedPluginWithPrinterRunner<T, List<InvalidPluginFile>>) {
    val pluginId = "com.example.intellij"
    val prefix = "com.example"
    val invalidPluginFiles = listOf(
      InvalidPluginFile(Path("plugin.zip"), listOf(ForbiddenPluginIdPrefix(PLUGIN_XML, pluginId, prefix)))
    )

    testRunner.run(resultPrinter, invalidPluginFiles)
  }

  fun output() = out.buffer.toString()

  private fun printResults(verificationResult: PluginVerificationResult) {
    resultPrinter.printResults(listOf(verificationResult))
  }

  fun assertOutput(expected: String) {
    assertEquals(expected, output())
  }

  private fun VerifiedPluginHandler.runTest(verificationResult: PluginVerificationResult.Verified) {
    printResults(verificationResult)
    this(verificationResult)
  }

  private fun mockCompatibilityProblems(): Set<CompatibilityProblem> =
    setOf(superInterfaceBecameClassProblem(), superInterfaceBecameClassProblemInOtherLocation(), methodNotFoundProblem(), methodNotFoundProblemInSampleStuffFactoryClass())
}