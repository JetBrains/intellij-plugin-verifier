/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests.dependencies

import com.jetbrains.plugin.structure.classes.resolvers.EMPTY_RESOLVER
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.Module
import com.jetbrains.plugin.structure.intellij.plugin.ModuleDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.ModuleLoadingRule
import com.jetbrains.plugin.structure.intellij.plugin.ModuleVisibility
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyEdge
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.dependencies.ModuleVisibilityChecker
import com.jetbrains.pluginverifier.dependencies.ModuleVisibilityChecker.ResolvedModuleInfoFrom
import com.jetbrains.pluginverifier.dependencies.ModuleVisibilityChecker.ResolvedModuleInfoTo
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.jdk.JdkDescriptor
import com.jetbrains.pluginverifier.jdk.JdkVersion
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginInfo
import com.jetbrains.pluginverifier.resolution.DefaultClassResolverProvider
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.problems.ModuleVisibilityProblem
import com.jetbrains.pluginverifier.tests.mocks.MockIde
import com.jetbrains.pluginverifier.tests.mocks.MockIdePlugin
import com.jetbrains.pluginverifier.tests.mocks.createPluginArchiveManager
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
import com.jetbrains.pluginverifier.verifiers.ProblemRegistrar
import com.jetbrains.pluginverifier.verifiers.packages.DefaultPackageFilter
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ModuleVisibilityCheckerTest {

  @JvmField
  @Rule
  var tempFolder: TemporaryFolder = TemporaryFolder()

  private lateinit var parentPluginA: MockIdePlugin
  private lateinit var parentPluginB: MockIdePlugin

  @Before
  fun setUp() {
    parentPluginA = MockIdePlugin(pluginId = "plugin.a", pluginVersion = "1.0")
    parentPluginB = MockIdePlugin(pluginId = "plugin.b", pluginVersion = "1.0")
  }

  // --- isAccessAllowed tests (using ResolvedModuleInfo data classes directly) ---

  @Test
  fun `PUBLIC visibility allows access from any module`() {
    val checker = createCheckerForVisibilityTests()

    val from = ResolvedModuleInfoFrom(parentPluginA, "namespace.a")
    val to = ResolvedModuleInfoTo(parentPluginB, "namespace.b", ModuleVisibility.PUBLIC)

    assertTrue("PUBLIC visibility should always allow access", checker.isAccessAllowed(from, to))
  }

  @Test
  fun `INTERNAL visibility allows access from same plugin`() {
    val checker = createCheckerForVisibilityTests()

    val from = ResolvedModuleInfoFrom(parentPluginA, "namespace.a")
    val to = ResolvedModuleInfoTo(parentPluginA, "namespace.a", ModuleVisibility.INTERNAL)

    assertTrue("INTERNAL visibility should allow access from same plugin", checker.isAccessAllowed(from, to))
  }

  @Test
  fun `INTERNAL visibility allows access from same namespace different plugin`() {
    val checker = createCheckerForVisibilityTests()

    val sharedNamespace = "com.jetbrains.shared"
    val from = ResolvedModuleInfoFrom(parentPluginA, sharedNamespace)
    val to = ResolvedModuleInfoTo(parentPluginB, sharedNamespace, ModuleVisibility.INTERNAL)

    assertTrue("INTERNAL visibility should allow access from same namespace", checker.isAccessAllowed(from, to))
  }

  @Test
  fun `INTERNAL visibility denies access from different namespace`() {
    val checker = createCheckerForVisibilityTests()

    val from = ResolvedModuleInfoFrom(parentPluginA, "namespace.a")
    val to = ResolvedModuleInfoTo(parentPluginB, "namespace.b", ModuleVisibility.INTERNAL)

    assertFalse("INTERNAL visibility should deny access from different namespace", checker.isAccessAllowed(from, to))
  }

  @Test
  fun `PRIVATE visibility allows access from same plugin`() {
    val checker = createCheckerForVisibilityTests()

    val from = ResolvedModuleInfoFrom(parentPluginA, "namespace.a")
    val to = ResolvedModuleInfoTo(parentPluginA, "namespace.a", ModuleVisibility.PRIVATE)

    assertTrue("PRIVATE visibility should allow access from same plugin", checker.isAccessAllowed(from, to))
  }

  @Test
  fun `PRIVATE visibility denies access from different plugin`() {
    val checker = createCheckerForVisibilityTests()

    val from = ResolvedModuleInfoFrom(parentPluginA, "namespace.a")
    val to = ResolvedModuleInfoTo(parentPluginB, "namespace.a", ModuleVisibility.PRIVATE)

    assertFalse("PRIVATE visibility should deny access from different plugin", checker.isAccessAllowed(from, to))
  }

  @Test
  fun `PRIVATE visibility denies access from different plugin even with same namespace`() {
    val checker = createCheckerForVisibilityTests()

    val sharedNamespace = "com.jetbrains.shared"
    val from = ResolvedModuleInfoFrom(parentPluginA, sharedNamespace)
    val to = ResolvedModuleInfoTo(parentPluginB, sharedNamespace, ModuleVisibility.PRIVATE)

    assertFalse("PRIVATE visibility should deny access even with same namespace", checker.isAccessAllowed(from, to))
  }

  @Test
  fun `isApplicable returns false for IDE version below 261`() {
    val context = createMockPluginVerificationContext(IdeVersion.createIdeVersion("IU-253.1"))
    assertFalse("Should not be applicable for IDE version < 261", ModuleVisibilityChecker.supports(context))
  }

  @Test
  fun `build throws when isApplicable is false`() {
    val context = createMockPluginVerificationContext(IdeVersion.createIdeVersion("IU-253.1"))
    try {
      ModuleVisibilityChecker.build(context)
      fail("Expected IllegalStateException when build() is called on an inapplicable context")
    } catch (e: IllegalStateException) {
      // expected
    }
  }

  // --- checkEdges: only direct dependencies of the verified plugin are checked ---

  @Test
  fun `checkEdges reports a visibility problem for a direct dependency but not for a transitive one`() {
    // Graph: verified plugin A → private module B → private module C
    //
    // Without the guard `a != dependenciesGraph.verifiedPlugin`, both A→B and B→C would be
    //    // checked and two ModuleVisibilityProblems would be reported.
    //    // With the guard, only A→B is checked: B→C is a transitive edge and must be skipped.

    // Set up plugin B with a PRIVATE module so resolveModuleInfoTo(B) succeeds.
    val pluginB = pluginWithPrivateModule("plugin.b", "com.example.b")
    // Set up plugin C likewise (ensures B→C *would* have been caught without the fix).
    val pluginC = pluginWithPrivateModule("plugin.c", "com.example.c")

    val nodeA = DependencyNode("plugin.a", "1.0", parentPluginA) // verified plugin (no module descriptors → uses MODULE_PLACEHOLDER_STRING)
    val nodeB = DependencyNode("plugin.b", "1.0", pluginB)
    val nodeC = DependencyNode("plugin.c", "1.0", pluginC)

    val graph = DependenciesGraph(
      verifiedPlugin = nodeA,
      vertices = setOf(nodeA, nodeB, nodeC),
      edges = setOf(
        DependencyEdge(nodeA, nodeB, PluginDependencyImpl("plugin.b", false, false)), // direct
        DependencyEdge(nodeB, nodeC, PluginDependencyImpl("plugin.c", false, false))  // transitive
      ),
      missingDependencies = emptyMap()
    )

    // Build checker with parentPluginA as the verified (main) plugin.
    val context = createMockPluginVerificationContext(IdeVersion.createIdeVersion("IU-261.1"), parentPluginA)
    val checker = ModuleVisibilityChecker.build(context)

    val problems = mutableListOf<CompatibilityProblem>()
    val registrar = object : ProblemRegistrar {
      override fun registerProblem(problem: CompatibilityProblem) = problems.add(problem).let {}
    }

    checker.checkEdges(graph, registrar)

    val visibilityProblems = problems.filterIsInstance<ModuleVisibilityProblem>()
    assertEquals("Exactly one visibility problem should be reported (for the direct A→B edge)", 1, visibilityProblems.size)
    assertEquals("plugin.b", visibilityProblems.single().targetModuleName)
    assertFalse(
      "No problem should be reported for the transitive B→C edge",
      visibilityProblems.any { it.targetModuleName == "plugin.c" }
    )
  }

  /**
   * Creates a [MockIdePlugin] that exposes a single PRIVATE content module, making
   * [ModuleVisibilityChecker.resolveModuleInfoTo] return a non-null result.
   */
  private fun pluginWithPrivateModule(pluginId: String, namespace: String): MockIdePlugin {
    val modulePlugin = MockIdePlugin(pluginId = pluginId) // moduleVisibility defaults to PRIVATE in MockIdePlugin
    val descriptor = ModuleDescriptor(
      module = modulePlugin,
      configurationFilePath = "$pluginId.xml",
      moduleDefinition = Module.FileBasedModule(pluginId, namespace, namespace, ModuleLoadingRule.REQUIRED, "$pluginId.xml")
    )
    return MockIdePlugin(pluginId = pluginId, pluginVersion = "1.0", modulesDescriptors = listOf(descriptor))
  }

  // --- Helper methods ---

  private fun createCheckerForVisibilityTests(): ModuleVisibilityChecker {
    val ideVersion = IdeVersion.createIdeVersion("IU-261.1")
    val mainPlugin = MockIdePlugin(pluginId = "main.plugin", pluginVersion = "1.0")
    val context = createMockPluginVerificationContext(ideVersion, mainPlugin, listOf(mainPlugin))
    return ModuleVisibilityChecker.build(context)
  }

  private fun createMockPluginVerificationContext(
    ideVersion: IdeVersion,
    mainPlugin: IdePlugin = MockIdePlugin(pluginId = "test.plugin", pluginVersion = "1.0"),
    bundledPlugins: List<IdePlugin> = listOf(mainPlugin)
  ): PluginVerificationContext {
    val idePath = tempFolder.newFolder("ide").toPath()

    val ide = MockIde(ideVersion, idePath, bundledPlugins)

    val jdkPath = tempFolder.newFolder("jdk").toPath()
    val jdkVersion = JdkVersion("17", null)
    val jdkDescriptor = JdkDescriptor(jdkPath, EMPTY_RESOLVER, jdkVersion)

    val ideDescriptor = IdeDescriptor(ide, EMPTY_RESOLVER, jdkDescriptor, ideFileLock = null)

    val classResolverProvider = DefaultClassResolverProvider(
      dependencyFinder = MockDependencyFinder(),
      ideDescriptor = ideDescriptor,
      externalClassesPackageFilter = DefaultPackageFilter(emptyList()),
      additionalClassResolvers = emptyList(),
      archiveManager = tempFolder.createPluginArchiveManager()
    )

    val verificationDescriptor = PluginVerificationDescriptor.IDE(
      ideDescriptor,
      classResolverProvider,
      LocalPluginInfo(mainPlugin)
    )

    val dependenciesGraph = DependenciesGraph(
      verifiedPlugin = DependencyNode(mainPlugin.pluginId ?: "unknown", mainPlugin.pluginVersion ?: "unknown", mainPlugin),
      vertices = emptySet(),
      edges = emptySet(),
      missingDependencies = emptyMap()
    )

    return PluginVerificationContext(
      idePlugin = mainPlugin,
      verificationDescriptor = verificationDescriptor,
      pluginResolver = EMPTY_RESOLVER,
      allResolver = EMPTY_RESOLVER,
      externalClassesPackageFilter = DefaultPackageFilter(emptyList()),
      dependenciesGraph = dependenciesGraph
    )
  }

  private class MockDependencyFinder : DependencyFinder {
    override val presentableName: String = "Mock Dependency Finder"

    override fun findPluginDependency(
      dependencyId: String,
      isModule: Boolean
    ): DependencyFinder.Result =
      DependencyFinder.Result.NotFound("Mock: not found")

    override fun findPluginDependency(dependency: PluginDependency): DependencyFinder.Result =
      DependencyFinder.Result.NotFound("Mock: not found")
  }
}
