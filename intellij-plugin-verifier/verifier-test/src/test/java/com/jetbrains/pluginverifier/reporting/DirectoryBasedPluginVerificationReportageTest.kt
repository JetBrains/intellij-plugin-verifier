/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.reporting

import com.jetbrains.plugin.structure.base.problems.InvalidPluginIDProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.dependencies.ResolvedDependenciesGraph
import com.jetbrains.pluginverifier.dependencies.ResolvedDependencyNode
import com.jetbrains.pluginverifier.dependencies.presentation.ResolvedDependenciesGraphPrettyPrinter
import com.jetbrains.pluginverifier.jdk.JdkVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.warnings.PluginStructureError
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.*

class DirectoryBasedPluginVerificationReportageTest {
  private lateinit var verificationDirectory: Path
  private lateinit var executor: ExecutorService

  private val verificationTarget = PluginVerificationTarget.IDE(IdeVersion.createIdeVersion("232"), JdkVersion("11", null))

  private val targetDirectoryProvider: (PluginVerificationTarget) -> Path = { verificationDirectory.resolve(it.toString()) }

  @Before
  fun setUp() {
    verificationDirectory = createTempDirectory()
    executor = Executors.newFixedThreadPool(THREADS)
  }

  @OptIn(kotlin.io.path.ExperimentalPathApi::class)
  @After
  fun tearDown() {
    executor.shutdownNow()
    verificationDirectory.deleteRecursively()
  }

  @Test
  fun `reports of different plugins are written concurrently`() {
    val parallelism = 4
    val allInside = CountDownLatch(parallelism)
    val timedOutWorkers = AtomicInteger()
    val reportage = DirectoryBasedPluginVerificationReportage { target ->
      allInside.countDown()
      if (!allInside.await(2, TimeUnit.SECONDS)) {
        timedOutWorkers.incrementAndGet()
      }
      targetDirectoryProvider(target)
    }

    val results = (0 until parallelism).map { verified(pluginInfo("plugin$it")) }
    reportage.use {
      runConcurrently(results.map { result -> { reportage.reportVerificationResult(result) } })
    }

    assertEquals(
      "All $parallelism workers must be inside 'reportVerificationResult' at the same time",
      0, timedOutWorkers.get()
    )
  }

  @Test
  fun `all report files are written when reporting concurrently`() {
    val results = (0 until PLUGIN_COUNT).map { verified(pluginInfo("plugin$it")) }

    DirectoryBasedPluginVerificationReportage(targetDirectoryProvider = targetDirectoryProvider).use { reportage ->
      runConcurrently(results.map { result -> { reportage.reportVerificationResult(result) } })
    }

    results.forEach { result ->
      val pluginDirectory = pluginDirectory(result.plugin)
      assertEquals(listOf(result.verificationVerdict), pluginDirectory.resolve("verification-verdict.txt").readLines())
      assertEquals(
        ResolvedDependenciesGraphPrettyPrinter(result.dependenciesGraph).prettyPresentation() + "\n",
        pluginDirectory.resolve("dependencies.txt").readText()
      )
    }
  }

  @Test
  fun `aggregator receives exactly one serialized call per verified or invalid plugin result`() {
    val aggregator = RecordingAggregator()
    val verified = (0 until PLUGIN_COUNT).map { verified(pluginInfo("verified$it")) }
    val invalid = (0 until PLUGIN_COUNT).map { invalid(pluginInfo("invalid$it")) }
    val notFound = (0 until PLUGIN_COUNT).map { PluginVerificationResult.NotFound(pluginInfo("notFound$it"), verificationTarget, "not found") }
    val results = verified + invalid + notFound

    DirectoryBasedPluginVerificationReportage(aggregator, targetDirectoryProvider = targetDirectoryProvider).use { reportage ->
      runConcurrently(results.shuffled().map { result -> { reportage.reportVerificationResult(result) } })
    }

    assertFalse("Aggregator calls must not overlap", aggregator.overlapDetected)
    assertEquals((verified + invalid).map { it.plugin.pluginId }.sorted(), aggregator.handledPluginIds.sorted())

    invalid.forEach {
      assertTrue(pluginDirectory(it.plugin).resolve("invalid-plugin.txt").exists())
    }
    notFound.forEach {
      val pluginDirectory = pluginDirectory(it.plugin)
      assertEquals(listOf(it.verificationVerdict), pluginDirectory.resolve("verification-verdict.txt").readLines())
      assertFalse(pluginDirectory.resolve("invalid-plugin.txt").exists())
    }
  }

  @Test
  fun `all ignored problems are collected when reporting concurrently`() {
    val results = (0 until PLUGIN_COUNT).map {
      verified(pluginInfo("plugin$it"), ignoredProblems = mapOf(TestProblem("ignored-$it") to "ignored on purpose"))
    }

    DirectoryBasedPluginVerificationReportage(targetDirectoryProvider = targetDirectoryProvider).use { reportage ->
      runConcurrently(results.map { result -> { reportage.reportVerificationResult(result) } })
    }

    val allIgnoredProblems = targetDirectoryProvider(verificationTarget).resolve("all-ignored-problems.txt").readText()
    (0 until PLUGIN_COUNT).forEach {
      assertTrue("Missing ignored problem #$it", allIgnoredProblems.contains("Test problem ignored-$it\n"))
    }
    results.forEach {
      val ignoredProblems = pluginDirectory(it.plugin).resolve("ignored-problems.txt").readText()
      assertTrue(ignoredProblems.contains("Test problem ignored-${it.plugin.pluginId.removePrefix("plugin")}"))
    }
  }

  @Test
  fun `reports of the same plugin directory do not interleave`() {
    val plugin = pluginInfo("samePlugin")
    val first = verified(plugin, compatibilityProblems = setOf(TestProblem("first")))
    val second = verified(plugin, compatibilityProblems = setOf(TestProblem("second-1"), TestProblem("second-2")))
    val expectedFiles = listOf(first, second).map { result ->
      listOf(result.verificationVerdict) to result.compatibilityProblems.map { it.fullDescription }.sorted()
    }

    repeat(SAME_DIRECTORY_ROUNDS) {
      DirectoryBasedPluginVerificationReportage(targetDirectoryProvider = targetDirectoryProvider).use { reportage ->
        runConcurrently(listOf(first, second).map { result -> { reportage.reportVerificationResult(result) } })
      }

      val pluginDirectory = pluginDirectory(plugin)
      val actualFiles = pluginDirectory.resolve("verification-verdict.txt").readLines() to
        pluginDirectory.resolve("compatibility-problems.txt").readLines().sorted()
      assertTrue("Unexpected report contents: $actualFiles", actualFiles in expectedFiles)
    }
  }

  private fun runConcurrently(tasks: List<() -> Unit>) {
    val start = CountDownLatch(1)
    val futures = tasks.map { task ->
      executor.submit {
        start.await()
        task()
      }
    }
    start.countDown()
    futures.forEach { it.get(30, TimeUnit.SECONDS) }
  }

  private fun pluginDirectory(plugin: PluginInfo): Path =
    targetDirectoryProvider(verificationTarget).resolve("plugins").resolve(plugin.pluginId).resolve(plugin.version)

  private fun verified(
    plugin: PluginInfo,
    compatibilityProblems: Set<CompatibilityProblem> = emptySet(),
    ignoredProblems: Map<CompatibilityProblem, String> = emptyMap()
  ): PluginVerificationResult.Verified {
    val dependenciesGraph = ResolvedDependenciesGraph(
      verifiedPlugin = ResolvedDependencyNode(plugin.pluginId, plugin.version),
      vertices = emptySet(),
      edges = emptySet(),
      missingDependencies = emptyMap()
    )
    return PluginVerificationResult.Verified(
      plugin,
      verificationTarget,
      dependenciesGraph,
      compatibilityProblems = compatibilityProblems,
      ignoredProblems = ignoredProblems
    )
  }

  private fun invalid(plugin: PluginInfo): PluginVerificationResult.InvalidPlugin {
    val pluginStructureErrors = setOf(PluginStructureError(InvalidPluginIDProblem(IdePluginManager.PLUGIN_XML)))
    return PluginVerificationResult.InvalidPlugin(plugin, verificationTarget, pluginStructureErrors)
  }

  private fun pluginInfo(pluginId: String): PluginInfo =
    object : PluginInfo(pluginId, pluginId, PLUGIN_VERSION, null, null, null) {}

  /**
   * Deliberately not thread-safe: relies on the reportage serializing the aggregator calls.
   */
  private class RecordingAggregator : PluginVerificationReportageAggregator {
    val handledPluginIds = arrayListOf<String>()
    private val inFlight = AtomicInteger()
    @Volatile
    var overlapDetected = false

    override fun handleVerificationResult(result: PluginVerificationResult, targetDirectory: Path) {
      if (inFlight.incrementAndGet() != 1) {
        overlapDetected = true
      }
      Thread.yield()
      handledPluginIds += result.plugin.pluginId
      inFlight.decrementAndGet()
    }
  }

  private class TestProblem(private val id: String) : CompatibilityProblem() {
    override val problemType = "Test problem"
    override val shortDescription = "Test problem"
    override val fullDescription = "Test problem $id"
    override fun equals(other: Any?) = other is TestProblem && id == other.id
    override fun hashCode() = id.hashCode()
  }

  private companion object {
    const val THREADS = 8
    const val PLUGIN_COUNT = 50
    const val SAME_DIRECTORY_ROUNDS = 20
    const val PLUGIN_VERSION = "1.0"
  }
}
