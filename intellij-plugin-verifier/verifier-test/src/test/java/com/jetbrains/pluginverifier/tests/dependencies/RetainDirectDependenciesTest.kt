/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests.dependencies

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.dependencies.*
import com.jetbrains.pluginverifier.dymamic.DynamicPluginStatus
import com.jetbrains.pluginverifier.jdk.JdkVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.retainDirectDependencies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class RetainDirectDependenciesTest {
  private val plugin = ResolvedDependencyNode("plugin", "1.0")
  private val platform = ResolvedDependencyNode("com.intellij", "261.1", setOf("com.intellij.modules.platform", "com.intellij.modules.lang"))
  private val javaPlugin = ResolvedDependencyNode("com.intellij.java", "261.1")
  private val module = ResolvedDependencyNode("com.intellij.java.core", "261.1", moduleOwnerId = "com.intellij.java")
  private val kotlin = ResolvedDependencyNode("org.jetbrains.kotlin", "261.1")
  private val xml = ResolvedDependencyNode("com.intellij.xml", "261.1")

  private val onPlatform = ResolvedPluginDependency("com.intellij.modules.platform", isOptional = false, isModule = true)
  private val onJava = ResolvedPluginDependency("com.intellij.java", isOptional = false, isModule = false)
  private val onJavaCore = ResolvedPluginDependency("com.intellij.java.core", isOptional = false, isModule = true, isContentModule = true)
  private val onXml = ResolvedPluginDependency("com.intellij.xml", isOptional = false, isModule = false)
  private val onMissing = ResolvedPluginDependency("org.missing", isOptional = true, isModule = false)
  private val onKotlin = ResolvedPluginDependency("org.jetbrains.kotlin", isOptional = false, isModule = false)

  private val directToPlatform = ResolvedDependencyEdge(plugin, platform, onPlatform)
  private val directToJava = ResolvedDependencyEdge(plugin, javaPlugin, onJava)
  private val directToModule = ResolvedDependencyEdge(plugin, module, onJavaCore)
  private val transitiveToPlatform = ResolvedDependencyEdge(javaPlugin, platform, onPlatform)
  private val transitiveToXml = ResolvedDependencyEdge(javaPlugin, xml, onXml)
  private val transitiveToKotlin = ResolvedDependencyEdge(xml, kotlin, onKotlin)

  // The plugin misses 'org.missing', which is resolved for the Kotlin plugin.
  private val missingOfPlugin = ResolvedMissingDependency(onMissing, "Plugin org.missing is not found")
  private val missingOfKotlin = ResolvedMissingDependency(onXml, "Plugin com.intellij.xml is disabled")
  private val transitiveToMissing = ResolvedDependencyEdge(kotlin, ResolvedDependencyNode("org.missing", "1.0"), onMissing)

  private val graph = ResolvedDependenciesGraph(
    plugin,
    setOf(plugin, platform, javaPlugin, module, kotlin, xml, transitiveToMissing.to),
    java.util.Set.of(directToPlatform, directToJava, directToModule, transitiveToPlatform, transitiveToXml, transitiveToKotlin, transitiveToMissing),
    mapOf(plugin to setOf(missingOfPlugin), kotlin to setOf(missingOfKotlin))
  )

  @Test
  fun `retains direct edges, edges of direct dependencies and direct missing dependencies`() {
    val retained = graph.retainDirectDependencies()

    assertSame(plugin, retained.verifiedPlugin)
    assertEquals(setOf(directToPlatform, directToJava, directToModule, transitiveToPlatform, transitiveToMissing), retained.edges)
    assertEquals(setOf(plugin, platform, javaPlugin, module, kotlin, transitiveToMissing.to), retained.vertices)
    assertEquals(mapOf(plugin to setOf(missingOfPlugin)), retained.missingDependencies)
    assertEquals(graph.getDirectMissingDependencies(), retained.getDirectMissingDependencies())
  }

  @Test
  fun `direct edges come first so that a lookup of a direct dependency finds the edge of the verified plugin`() {
    val retained = graph.retainDirectDependencies()

    assertEquals(setOf(directToPlatform, directToJava, directToModule), retained.edges.take(3).toSet())
    assertSame(directToPlatform, retained.edges.find { it.dependency == onPlatform })
    assertSame(platform.aliases, retained.edges.find { it.dependency == onPlatform }!!.to.aliases)
  }

  @Test
  fun `graph without direct missing dependencies retains no missing dependencies`() {
    val withoutMissing = ResolvedDependenciesGraph(plugin, setOf(plugin, javaPlugin), setOf(directToJava), mapOf(javaPlugin to setOf(missingOfKotlin)))

    val retained = withoutMissing.retainDirectDependencies()

    assertEquals(setOf(directToJava), retained.edges)
    assertEquals(setOf(plugin, javaPlugin), retained.vertices)
    assertEquals(emptyMap<ResolvedDependencyNode, Set<ResolvedMissingDependency>>(), retained.missingDependencies)
  }

  @Test
  fun `graph without edges retains the verified plugin only`() {
    val retained = ResolvedDependenciesGraph(plugin, emptySet(), emptySet(), emptyMap()).retainDirectDependencies()

    assertEquals(ResolvedDependenciesGraph(plugin, setOf(plugin), emptySet(), emptyMap()), retained)
  }

  @Test
  fun `verified result retains direct dependencies and keeps all other data and the verdict`() {
    val result = PluginVerificationResult.Verified(
      pluginInfo,
      verificationTarget,
      graph,
      ignoredProblems = emptyMap(),
      dynamicPluginStatus = DynamicPluginStatus.MaybeDynamic
    )

    val retained = result.retainDirectDependencies() as PluginVerificationResult.Verified

    assertEquals(graph.retainDirectDependencies(), retained.dependenciesGraph)
    assertEquals(result.verificationVerdict, retained.verificationVerdict)
    assertEquals(result.directMissingMandatoryDependencies, retained.directMissingMandatoryDependencies)
    assertSame(result.plugin, retained.plugin)
    assertSame(result.verificationTarget, retained.verificationTarget)
    assertSame(result.compatibilityProblems, retained.compatibilityProblems)
    assertSame(result.ignoredProblems, retained.ignoredProblems)
    assertSame(result.compatibilityWarnings, retained.compatibilityWarnings)
    assertSame(result.deprecatedUsages, retained.deprecatedUsages)
    assertSame(result.experimentalApiUsages, retained.experimentalApiUsages)
    assertSame(result.internalApiUsages, retained.internalApiUsages)
    assertSame(result.ignoredInternalApiUsages, retained.ignoredInternalApiUsages)
    assertSame(result.nonExtendableApiUsages, retained.nonExtendableApiUsages)
    assertSame(result.overrideOnlyMethodUsages, retained.overrideOnlyMethodUsages)
    assertSame(result.pluginStructureWarnings, retained.pluginStructureWarnings)
    assertSame(result.dynamicPluginStatus, retained.dynamicPluginStatus)
    assertSame(result.telemetry, retained.telemetry)
  }

  @Test
  fun `results other than verified are retained as is`() {
    val notFound = PluginVerificationResult.NotFound(pluginInfo, verificationTarget, "not found")
    val invalid = PluginVerificationResult.InvalidPlugin(pluginInfo, verificationTarget, emptySet())

    assertSame(notFound, notFound.retainDirectDependencies())
    assertSame(invalid, invalid.retainDirectDependencies())
  }

  private val pluginInfo: PluginInfo = object : PluginInfo("plugin", "plugin", "1.0", null, null, null) {}

  private val verificationTarget = PluginVerificationTarget.IDE(IdeVersion.createIdeVersion("261.1"), JdkVersion("17", null))
}
