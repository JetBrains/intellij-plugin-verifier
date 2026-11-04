/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.plugin.structure.base.problems.InvalidPluginIDProblem
import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.dependencies.ResolvedDependenciesGraph
import com.jetbrains.pluginverifier.dependencies.ResolvedDependencyNode
import com.jetbrains.pluginverifier.jdk.JdkVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.ElementType
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.location.toReference
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.usages.internal.InternalApiUsage
import com.jetbrains.pluginverifier.usages.internal.InternalMethodUsage
import com.jetbrains.pluginverifier.warnings.PluginStructureError
import org.junit.Assert.assertEquals
import org.junit.Test

class InternalApiUsagesSummaryTest {

  private val target = PluginVerificationTarget.IDE(IdeVersion.createIdeVersion("IU-263.SNAPSHOT"), JdkVersion("21", null))

  @Test
  fun `ranks internal APIs by distinct affected plugins and keeps ignored usages`() {
    val firstApiFromFirstPlugin = internalMethodUsage("com.intellij.FirstApi", "call", "plugin.one.FirstCaller")
    val firstApiSecondCallSite = internalMethodUsage("com.intellij.FirstApi", "call", "plugin.one.SecondCaller")
    val firstApiIgnoredFromSecondPlugin = internalMethodUsage("com.intellij.FirstApi", "call", "plugin.two.Caller")
    val secondApi = internalMethodUsage("com.intellij.SecondApi", "call", "plugin.three.Caller")

    val summary = InternalApiUsagesSummary.from(
      target,
      listOf(
        verifiedResult(
          pluginId = "plugin.one",
          internalApiUsages = setOf(firstApiFromFirstPlugin, firstApiSecondCallSite)
        ),
        verifiedResult(
          pluginId = "plugin.two",
          ignoredInternalApiUsages = mapOf(firstApiIgnoredFromSecondPlugin to "known internal API usage")
        ),
        verifiedResult(
          pluginId = "plugin.three",
          internalApiUsages = setOf(secondApi)
        ),
        PluginVerificationResult.InvalidPlugin(pluginInfo("plugin.invalid"), target,
          setOf(PluginStructureError(InvalidPluginIDProblem("plugin.invalid")))
        ),
        PluginVerificationResult.NotFound(pluginInfo("plugin.notFound"), target, "not found"),
        PluginVerificationResult.FailedToDownload(pluginInfo("plugin.failed"), target, "download failed")
      )
    )

    assertEquals(1, summary.schemaVersion)
    assertEquals("IU-263.SNAPSHOT", summary.ideVersion)
    assertEquals(3, summary.totals.checkedPluginCount)
    assertEquals(1, summary.totals.invalidPluginCount)
    assertEquals(1, summary.totals.notFoundPluginCount)
    assertEquals(1, summary.totals.failedToDownloadPluginCount)
    assertEquals(3, summary.totals.affectedPluginCount)
    assertEquals(2, summary.totals.distinctApiElementCount)
    assertEquals(4, summary.totals.usageCount)
    assertEquals(3, summary.totals.nonIgnoredUsageCount)
    assertEquals(1, summary.totals.ignoredUsageCount)

    val first = summary.apiElements[0]
    assertEquals(1, first.rank)
    assertEquals("com.intellij.FirstApi", first.apiElement.className)
    assertEquals("call", first.apiElement.memberName)
    assertEquals("()V", first.apiElement.descriptor)
    assertEquals(ElementType.METHOD, first.apiElement.apiElementType)
    assertEquals(2, first.affectedPluginCount)
    assertEquals(3, first.usageCount)
    assertEquals(2, first.nonIgnoredUsageCount)
    assertEquals(1, first.ignoredUsageCount)
    assertEquals(listOf("plugin.one", "plugin.two"), first.pluginIds)
    assertEquals(listOf("known internal API usage"), first.ignoreReasons)

    val second = summary.apiElements[1]
    assertEquals(2, second.rank)
    assertEquals("com.intellij.SecondApi", second.apiElement.className)
    assertEquals(1, second.affectedPluginCount)
  }

  private fun verifiedResult(
    pluginId: String,
    internalApiUsages: Set<InternalApiUsage> = emptySet(),
    ignoredInternalApiUsages: Map<InternalApiUsage, String> = emptyMap()
  ): PluginVerificationResult.Verified {
    val dependenciesGraph = ResolvedDependenciesGraph(
      verifiedPlugin = ResolvedDependencyNode(pluginId, "1.0"),
      vertices = emptySet(),
      edges = emptySet(),
      missingDependencies = emptyMap()
    )
    return PluginVerificationResult.Verified(
      plugin = pluginInfo(pluginId),
      verificationTarget = target,
      dependenciesGraph = dependenciesGraph,
      internalApiUsages = internalApiUsages,
      ignoredInternalApiUsages = ignoredInternalApiUsages
    )
  }

  private fun pluginInfo(pluginId: String) = object : PluginInfo(pluginId, pluginId, "1.0", null, null, null) {}

  private fun internalMethodUsage(apiClassName: String, methodName: String, callerClassName: String): InternalMethodUsage {
    val apiClass = classLocation(apiClassName)
    val apiMethod = MethodLocation(
      hostClass = apiClass,
      methodName = methodName,
      methodDescriptor = "()V",
      parameterNames = emptyList(),
      signature = null,
      modifiers = PUBLIC_MODIFIERS
    )
    val callerMethod = MethodLocation(
      hostClass = classLocation(callerClassName),
      methodName = "caller",
      methodDescriptor = "()V",
      parameterNames = emptyList(),
      signature = null,
      modifiers = PUBLIC_MODIFIERS
    )
    return InternalMethodUsage(apiMethod.toReference(), apiMethod, callerMethod)
  }

  private fun classLocation(className: String) = ClassLocation(className, null, PUBLIC_MODIFIERS, SomeFileOrigin)

  private object SomeFileOrigin : FileOrigin {
    override val parent: FileOrigin? = null
  }

  companion object {
    private val PUBLIC_MODIFIERS = Modifiers.of(Modifiers.Modifier.PUBLIC)
  }
}
