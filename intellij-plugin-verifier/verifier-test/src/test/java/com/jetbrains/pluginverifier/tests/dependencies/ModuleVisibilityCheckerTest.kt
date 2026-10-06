/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests.dependencies

import com.jetbrains.plugin.structure.classes.resolvers.EMPTY_RESOLVER
import com.jetbrains.plugin.structure.intellij.plugin.*
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.DependencyTree
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.IdPrefixIdeModulePredicate.Companion.HAS_COM_INTELLIJ_MODULE_PREFIX
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.dependencies.*
import com.jetbrains.pluginverifier.dependencies.DependencyNode.Companion.dependencyNode
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
import com.jetbrains.pluginverifier.tests.mocks.SimpleCompatibilityProblemRegistrar
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
    // Only declarations owned by the verified plugin should be checked:
    // A→B is checked, while B→C is a transitive edge and must be skipped.

    // Set up plugin B with a PRIVATE module so resolveModuleInfoTo(B) succeeds.
    val pluginB = pluginWithPrivateModule("plugin.b", "com.example.b")
    // Set up plugin C likewise (ensures B→C *would* have been caught without the fix).
    val pluginC = pluginWithPrivateModule("plugin.c", "com.example.c")

    val nodeA = dependencyNode(parentPluginA) // verified plugin (no module descriptors → uses MODULE_PLACEHOLDER_STRING)
    val nodeB = dependencyNode(pluginB)
    val nodeC = dependencyNode(pluginC)

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

  @Test
  fun `checkEdges reports a visibility problem for a private dependency declared only by a content module of the verified plugin`() {
    // The verified plugin `root` declares no dependencies in its main descriptor;
    // only its content module `root.m` depends on the private module of plugin.b.
    val rootModuleDependencies = listOf(ModuleV2Dependency("plugin.b"))
    val rootModule = Module.FileBasedModule("root.m", "com.example.root", "com.example.root", ModuleLoadingRule.REQUIRED, "root.m.xml")
    val rootModuleDescriptor = ModuleDescriptor.of(
      module = MockIdePlugin(pluginId = "root.m", pluginVersion = "1.0"),
      moduleDefinition = rootModule,
      resolvedDependencies = rootModuleDependencies
    )
    val root = MockIdePlugin(
      pluginId = "root",
      pluginVersion = "1.0",
      contentModules = listOf(rootModule),
      modulesDescriptors = listOf(rootModuleDescriptor)
    )

    // plugin.b keeps its content module `plugin.b` PRIVATE.
    val bundledPluginB = pluginWithPrivateModule("plugin.b", "com.example.b")

    val ide = MockIde(IdeVersion.createIdeVersion("IU-261.1"), bundledPlugins = listOf(bundledPluginB))
    val resolution = DependencyTree(ide, HAS_COM_INTELLIJ_MODULE_PREFIX).getDependencyTreeResolution(root)
    val graph = DependenciesGraphProvider().getDependenciesGraph(resolution)

    val context = createMockPluginVerificationContext(IdeVersion.createIdeVersion("IU-261.1"), root, listOf(root, bundledPluginB))
    val checker = ModuleVisibilityChecker.build(context)

    val registrar = SimpleCompatibilityProblemRegistrar()
    checker.checkEdges(graph, registrar)

    val problems = registrar.problems
    val visibilityProblems = problems.filterIsInstance<ModuleVisibilityProblem>()
    assertEquals(
      "Exactly one visibility problem should be reported for root.m -> plugin.b. Graph edges: ${graph.edges}",
      1,
      visibilityProblems.size
    )
    assertEquals("plugin.b", visibilityProblems.single().targetModuleName)
    assertEquals("root.m", visibilityProblems.single().dependingModuleName)
    assertEquals("root", visibilityProblems.single().dependingPluginId)
    assertEquals("plugin.b", visibilityProblems.single().targetPluginId)
    assertEquals("com.example.root", visibilityProblems.single().dependingNamespace)
    assertEquals("com.example.b", visibilityProblems.single().targetNamespace)
  }

  @Test
  fun `checkEdges resolves and reports module names independently of plugin IDs`() {
    val root = pluginWithModules("root", moduleDescriptor("root.m", "source.namespace", dependencies = listOf(ModuleV2Dependency("target.private"))))
    val target = pluginWithModules(
      "target.plugin",
      moduleDescriptor("target.public", "other.namespace", ModuleVisibility.PUBLIC),
      moduleDescriptor("target.private", "target.namespace")
    )

    val problem = checkModuleEdges(root, target).single()
    assertEquals("root.m", problem.dependingModuleName)
    assertEquals("root", problem.dependingPluginId)
    assertEquals("target.private", problem.targetModuleName)
    assertEquals("target.plugin", problem.targetPluginId)
    assertEquals(ModuleVisibility.PRIVATE, problem.targetVisibility)
    assertEquals("source.namespace", problem.dependingNamespace)
    assertEquals("target.namespace", problem.targetNamespace)
  }

  @Test
  fun `checkEdges resolves module dependencies from the main descriptor`() {
    val root = MockIdePlugin(pluginId = "root", pluginVersion = "1.0", contentModuleDependencies = listOf(ContentModuleDependency("target.private", "target.namespace")))
    val target = pluginWithModules("target.plugin", moduleDescriptor("target.private", "target.namespace"))

    val problem = checkModuleEdges(root, target).single()
    assertEquals("root", problem.dependingModuleName)
    assertEquals("target.private", problem.targetModuleName)
    assertEquals("target.plugin", problem.targetPluginId)
  }

  @Test
  fun `checkEdges allows INTERNAL access using the declaring module actual namespace`() {
    val root = pluginWithModules(
      "root",
      moduleDescriptor("root.first", "unrelated.namespace"),
      moduleDescriptor("root.m", null, dependencies = listOf(ModuleV2Dependency("target.internal")), actualNamespace = "shared.namespace")
    )
    val target = pluginWithModules("target.plugin", moduleDescriptor("target.internal", "shared.namespace", ModuleVisibility.INTERNAL))

    assertTrue(checkModuleEdges(root, target).isEmpty())
  }

  @Test
  fun `checkEdges uses the actual namespace for main descriptor dependencies`() {
    val root = pluginWithModules("root", moduleDescriptor("root.m", null, actualNamespace = "shared.namespace")).copy(
      contentModuleDependencies = listOf(ContentModuleDependency("target.internal", "shared.namespace"))
    )
    val target = pluginWithModules("target.plugin", moduleDescriptor("target.internal", "shared.namespace", ModuleVisibility.INTERNAL))

    assertTrue(checkModuleEdges(root, target).isEmpty())
  }

  @Test
  fun `checkEdges denies INTERNAL access despite a matching namespace on another source module`() {
    val root = pluginWithModules(
      "root",
      moduleDescriptor("root.first", "shared.namespace"),
      moduleDescriptor("root.m", null, dependencies = listOf(ModuleV2Dependency("target.internal")), actualNamespace = "root.implicit.namespace")
    )
    val target = pluginWithModules("target.plugin", moduleDescriptor("target.internal", "shared.namespace", ModuleVisibility.INTERNAL))

    val problem = checkModuleEdges(root, target).single()
    assertEquals("root.m", problem.dependingModuleName)
    assertEquals("root.implicit.namespace", problem.dependingNamespace)
    assertEquals("shared.namespace", problem.targetNamespace)
    assertEquals(ModuleVisibility.INTERNAL, problem.targetVisibility)
  }

  @Test
  fun `checkEdges allows PRIVATE access within the verified plugin`() {
    val root = pluginWithModules(
      "root",
      moduleDescriptor("root.m", "source.namespace", dependencies = listOf(ModuleV2Dependency("root.private"))),
      moduleDescriptor("root.private", "target.namespace")
    )

    assertTrue(checkModuleEdges(root).isEmpty())
  }

  @Test
  fun `checkEdges allows INTERNAL access within a plugin despite different namespaces`() {
    val root = pluginWithModules(
      "root",
      moduleDescriptor("root.m", "source.namespace", dependencies = listOf(ModuleV2Dependency("root.internal"))),
      moduleDescriptor("root.internal", "target.namespace", ModuleVisibility.INTERNAL)
    )

    assertTrue(checkModuleEdges(root).isEmpty())
  }

  @Test
  fun `checkEdges allows PUBLIC access across plugins`() {
    val root = pluginWithModules("root", moduleDescriptor("root.m", "source.namespace", dependencies = listOf(ModuleV2Dependency("target.public"))))
    val target = pluginWithModules("target.plugin", moduleDescriptor("target.public", "target.namespace", ModuleVisibility.PUBLIC))

    assertTrue(checkModuleEdges(root, target).isEmpty())
  }

  @Test
  fun `checkEdges excludes transitive content module declarations`() {
    val root = pluginWithModules("root", moduleDescriptor("root.m", "source.namespace", dependencies = listOf(ModuleV2Dependency("target.public"))))
    val target = pluginWithModules(
      "target.plugin",
      moduleDescriptor("target.public", "target.namespace", ModuleVisibility.PUBLIC, listOf(ModuleV2Dependency("transitive.private")))
    )
    val transitive = pluginWithModules("transitive.plugin", moduleDescriptor("transitive.private", "transitive.namespace"))

    assertTrue(checkModuleEdges(root, target, transitive).isEmpty())
  }

  @Test
  fun `checkEdges checks module reference sources owned by the verified plugin`() {
    val root = pluginWithModules("root", moduleDescriptor("root.m", "source.namespace"))
    val target = pluginWithModules("target.plugin", moduleDescriptor("target.private", "target.namespace"))
    val rootNode = dependencyNode(root)
    val source = DependencyNode.ModuleDependency(root, "root.m")
    val destination = DependencyNode.ModuleDependency(target, "target.private")
    val graph = DependenciesGraph(
      rootNode,
      setOf(rootNode, source, destination),
      setOf(DependencyEdge(source, destination, ModuleV2Dependency("target.private"))),
      emptyMap()
    )
    val checker = ModuleVisibilityChecker.build(createMockPluginVerificationContext(IdeVersion.createIdeVersion("IU-261.1"), root))
    val registrar = SimpleCompatibilityProblemRegistrar()

    checker.checkEdges(graph, registrar)

    val problem = registrar.problems.filterIsInstance<ModuleVisibilityProblem>().single()
    assertEquals("root.m", problem.dependingModuleName)
    assertEquals("target.private", problem.targetModuleName)
  }

  @Test
  fun `checkEdges skips ownership declarations`() {
    val root = pluginWithModules("root", moduleDescriptor("root.private", "source.namespace"))
    val rootNode = dependencyNode(root)
    val declaration = DependencyNode.ContentModuleDeclaration("root.private", root)
    val graph = DependenciesGraph(
      rootNode,
      setOf(rootNode, declaration),
      setOf(DependencyEdge(rootNode, declaration, ModuleV2Dependency("root.private"))),
      emptyMap()
    )
    val checker = ModuleVisibilityChecker.build(createMockPluginVerificationContext(IdeVersion.createIdeVersion("IU-261.1"), root))
    val registrar = SimpleCompatibilityProblemRegistrar()

    assertEquals(ResolvedModuleInfoFrom(root, "source.namespace"), checker.resolveModuleInfoFrom(declaration))
    assertNull("Ownership declarations must not be resolved as dependency targets", checker.resolveModuleInfoTo(declaration))
    checker.checkEdges(graph, registrar)

    assertTrue(registrar.problems.isEmpty())
  }

  @Test
  fun `checkEdges skips missing module descriptors without falling back to the plugin ID`() {
    val root = pluginWithModules("root", moduleDescriptor("root", "source.namespace"))
    val target = pluginWithModules("target.plugin", moduleDescriptor("target.plugin", "target.namespace"))
    val rootNode = dependencyNode(root)
    val missingSource = DependencyNode.ContentModuleDeclaration("root.missing", root)
    val missingTarget = DependencyNode.ModuleDependency(target, "target.missing")
    val targetNode = DependencyNode.ModuleDependency(target, "target.plugin")
    val graph = DependenciesGraph(
      rootNode,
      setOf(rootNode, missingSource, missingTarget, targetNode),
      setOf(
        DependencyEdge(missingSource, targetNode, ModuleV2Dependency("target.plugin")),
        DependencyEdge(rootNode, missingTarget, ModuleV2Dependency("target.missing"))
      ),
      emptyMap()
    )
    val checker = ModuleVisibilityChecker.build(createMockPluginVerificationContext(IdeVersion.createIdeVersion("IU-261.1"), root))
    val registrar = SimpleCompatibilityProblemRegistrar()

    checker.checkEdges(graph, registrar)

    assertTrue(registrar.problems.isEmpty())
  }

  private fun moduleDescriptor(
    name: String,
    namespace: String?,
    visibility: ModuleVisibility = ModuleVisibility.PRIVATE,
    dependencies: List<PluginDependency> = emptyList(),
    actualNamespace: String = namespace ?: "$name.namespace"
  ): ModuleDescriptor = ModuleDescriptor.of(
    module = MockIdePlugin(pluginId = name, moduleVisibility = visibility),
    moduleDefinition = Module.FileBasedModule(name, namespace, actualNamespace, ModuleLoadingRule.REQUIRED, "$name.xml"),
    resolvedDependencies = dependencies
  )

  private fun pluginWithModules(pluginId: String, vararg modules: ModuleDescriptor): MockIdePlugin = MockIdePlugin(
    pluginId = pluginId,
    pluginVersion = "1.0",
    contentModules = modules.map { it.moduleDefinition },
    modulesDescriptors = modules.toList()
  )

  private fun checkModuleEdges(root: IdePlugin, vararg dependencies: IdePlugin): List<ModuleVisibilityProblem> {
    val ideVersion = IdeVersion.createIdeVersion("IU-261.1")
    val bundledPlugins = listOf(root) + dependencies
    val ide = MockIde(ideVersion, bundledPlugins = bundledPlugins)
    val resolution = DependencyTree(ide, HAS_COM_INTELLIJ_MODULE_PREFIX).getDependencyTreeResolution(root)
    val graph = DependenciesGraphProvider().getDependenciesGraph(resolution)
    val checker = ModuleVisibilityChecker.build(createMockPluginVerificationContext(ideVersion, root, bundledPlugins))
    val registrar = SimpleCompatibilityProblemRegistrar()

    assertTrue("The test graph must contain a resolved module dependency", graph.edges.any { it.to is DependencyNode.ModuleDependency })
    assertTrue("All fixture dependencies should resolve", graph.missingDependencies.values.all { it.isEmpty() })
    checker.checkEdges(graph, registrar)

    return registrar.problems.filterIsInstance<ModuleVisibilityProblem>()
  }

  /**
   * Creates a [MockIdePlugin] that exposes a single PRIVATE content module, making
   * [ModuleVisibilityChecker.resolveModuleInfoTo] return a non-null result.
   */
  private fun pluginWithPrivateModule(pluginId: String, namespace: String): MockIdePlugin {
    val modulePlugin = MockIdePlugin(pluginId = pluginId, moduleVisibility = ModuleVisibility.PRIVATE)
    val moduleDefinition = Module.FileBasedModule(pluginId, namespace, namespace, ModuleLoadingRule.REQUIRED, "$pluginId.xml")
    val moduleDescriptor = ModuleDescriptor.of(
      module = modulePlugin,
      moduleDefinition = moduleDefinition
    )
    return MockIdePlugin(pluginId = pluginId, pluginVersion = "1.0", modulesDescriptors = listOf(moduleDescriptor), contentModules = listOf(moduleDefinition))
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
      verifiedPlugin = dependencyNode(mainPlugin),
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
