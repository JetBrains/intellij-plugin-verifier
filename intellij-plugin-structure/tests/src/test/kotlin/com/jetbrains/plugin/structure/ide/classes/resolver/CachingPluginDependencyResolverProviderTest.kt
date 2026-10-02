/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.classes.resolver

import com.jetbrains.plugin.structure.base.BinaryClassName
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.CharSequenceComparator
import com.jetbrains.plugin.structure.base.utils.binaryClassNames
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.createEmptyClass
import com.jetbrains.plugin.structure.base.utils.newTemporaryFile
import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.classes.IdeResolverConfiguration
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.plugin.Classpath
import com.jetbrains.plugin.structure.intellij.plugin.ContentModuleDependency
import com.jetbrains.plugin.structure.intellij.plugin.DefaultDependencyContributor
import com.jetbrains.plugin.structure.intellij.plugin.DependsPluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.Dependency
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.DependencyTree
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.IdPrefixIdeModulePredicate.Companion.HAS_COM_INTELLIJ_MODULE_PREFIX
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.NodeId
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.*
import net.bytebuddy.ByteBuddy
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path
import java.util.*

private const val UNKNOWN = ""

class CachingPluginDependencyResolverProviderTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var ideRoot: Path

  private lateinit var ideaCorePluginFile: Path

  private lateinit var ideaCorePlugin: IdePlugin
  private lateinit var javaPlugin: IdePlugin
  private lateinit var jsonPlugin: IdePlugin

  private lateinit var byteBuddy: ByteBuddy

  val expectedIdeaCorePluginPackages = binaryClassNames(
    "com" ,
    "com/intellij",
    "com/intellij/openapi",
    "com/intellij/openapi/graph",
    "com/intellij/openapi/graph/builder",
    "com/intellij/openapi/graph/builder/actions"
  )

  val expectedIdeaCorePluginExplicitPackages = binaryClassNames(
    "com/intellij/openapi/graph/builder/actions"
  )

  val expectedJavaPluginPackages = binaryClassNames(
    "com",
    "com/intellij",
    "com/intellij/openapi",
    "com/intellij/openapi/actionSystem",
  )

  val expectedJavaPluginExplicitPackages = binaryClassNames(
    "com/intellij/openapi/actionSystem",
  )

  private val expectedJsonPluginPackages = binaryClassNames(
    "com",
    "com/intellij",
    "com/intellij/json",
  )

  private val expectedJsonPluginExplicitPackages = binaryClassNames(
    "com/intellij/json",
  )

  private val expectedIdeaCoreClasses = binaryClassNames(
    "com/intellij/openapi/graph/builder/actions/SelectionNodeModeAction",
  )

  private val expectedJavaPluginClasses = binaryClassNames(
    "com/intellij/openapi/actionSystem/DataKeys",
  )

  private val expectedJsonPluginClasses = binaryClassNames(
    "com/intellij/json/JsonNamesValidator"
  )

  @Before
  fun setUp() {
    ideRoot = temporaryFolder.newFolder("idea").toPath()

    ideaCorePluginFile = buildZipFile(temporaryFolder.newTemporaryFile("idea/lib/product.jar")) {
      dirs("com/intellij/openapi/graph/builder/actions") {
        file("SelectionNodeModeAction.class", createEmptyClass("com/intellij/openapi/graph/builder/actions/SelectionNodeModeAction"))
      }
    }
    ideaCorePlugin = MockIdePlugin(
      pluginId = "com.intellij",
      pluginName = "IDEA CORE",
      originalFile = ideaCorePluginFile,
      pluginAliases = setOf(
        "com.intellij.modules.platform",
        "com.intellij.modules.lang"
      ),
      classpath = Classpath.of(listOf(ideaCorePluginFile))
    )

    val javaPluginFile = buildZipFile(temporaryFolder.newTemporaryFile("idea/plugins/java/lib/java-impl.jar")) {
      dirs("com/intellij/openapi/actionSystem") {
        file("DataKeys.class", createEmptyClass("com/intellij/openapi/actionSystem/DataKeys"))
      }
    }
    javaPlugin = MockIdePlugin(
      pluginId = "com.intellij.java",
      pluginName = "Java",
      originalFile = javaPluginFile,
      pluginAliases = setOf(
        "com.intellij.modules.java",
      ),
      contentModuleDependencies = listOf(
        ContentModuleDependency("com.intellij.modules.lang", "jetbrains")
      ),
      classpath = Classpath.of(listOf(javaPluginFile))
    )

    val jsonPluginFile = buildZipFile(temporaryFolder.newTemporaryFile("idea/plugins/json/lib/json.jar")) {
      dirs("com/intellij/json") {
        file("JsonNamesValidator.class", createEmptyClass("com/intellij/json/DataKeys/JsonNamesValidator"))
      }
    }
    jsonPlugin = MockIdePlugin(
      pluginId = "com.intellij.modules.json",
      pluginName = "JSON",
      originalFile = jsonPluginFile,
      contentModuleDependencies = listOf(
        ContentModuleDependency("com.intellij.modules.lang", "jetbrains")
      ),
      classpath = Classpath.of(listOf(jsonPluginFile))
    )
  }

  @Test
  fun `cache is used properly`() {
    val ideVersion = IdeVersion.createIdeVersion("IU-243.12818.47")

    val plugin = idePlugin("com.example.somePlugin") {
      depends("com.intellij.modules.platform")
      depends("com.intellij.modules.json")
    }
    val pluginDependingOnJava = idePlugin("com.example.BetterJava") {
      depends("com.intellij.modules.java")
    }

    val ide = MockIde(ideVersion, ideRoot, bundledPlugins = listOf(ideaCorePlugin, javaPlugin, jsonPlugin))

    val resolverProvider = CachingPluginDependencyResolverProvider(ide)
    val resolver = resolverProvider.getResolver(plugin)
    with(resolverProvider.getStats()) {
      assertNotNull(this); this!!
      /*
        "com.intellij.modules.platform" and "com.intellij.modules.lang" belong to the same module.
        Cache was not even hit, since the resolver for the second invocation is already in the list
        of modules since the first invocation
       */
      assertEquals(0, hitCount())
      /*
        1) "com.example.somePlugin" (plugin itself),
        2) "com.intellij" unlocked
        3) "com.intellij" under lock
        4) "com.intellij" sole 'classpath' entry ("com.intellij/product.jar")
        5) "com.intellij.modules.json" unlocked
        6) "com.intellij.modules.json" under lock
        7) "com.intellij.modules.json" sole 'classpath' entry ("com.intellij.modules.json/json.jar")
        The modules of "com.intellij" are not considered to be cache misses as they are conflated with the
        "com.intellij" plugin.
        - "com.intellij.modules.platform" as module of "com.intellij"
        - "com.intellij.modules.lang" as a module of "com.intellij"
       */
      assertEquals(7,  missCount())
    }
    listOf("com.example.somePlugin")
      .forEach {
        assertTrue("Dependency resolver must cache $it", resolverProvider.dependencyResolverCacheContains(it))
      }
    listOf(
      "com.intellij",
      "com.intellij/product.jar",
      "com.intellij.modules.json",
      "com.intellij.modules.json/json.jar",
      "com.intellij.modules.platform",
      "com.intellij.modules.lang")
      .forEach {
        assertTrue("Plugin resolver must cache $it", resolverProvider.pluginResolverCacheContains(it))
      }

    with(resolverProvider.getStats()) {
      assertNotNull(this); this!!
      // Inspecting the cache contents above does not affect cache statistics.
      assertEquals(0, hitCount())
    }

    with(resolver) {
      assertEquals(expectedIdeaCorePluginExplicitPackages + expectedJsonPluginExplicitPackages, packages)
      assertEquals(expectedIdeaCoreClasses + expectedJsonPluginClasses, allClassNames)
    }

    val pluginDependingOnJavaResolver = resolverProvider.getResolver(pluginDependingOnJava)

    with(resolverProvider.getStats()) {
      assertNotNull(this); this!!
      // The resolver for "com.intellij" is already cached.
      assertEquals(1, hitCount())
      /*
        Existing 7 from the previous half of the test, plus:
        8) "com.example.BetterJava" (plugin itself)
        9) "com.intellij.java" unlocked
        10) "com.intellij.java" under lock
        11) "com.intellij.java" sole 'classpath' entry ("com.intellij.java/java-impl.jar")
       */
      assertEquals(11, missCount())
    }

    with(pluginDependingOnJavaResolver) {
      assertEquals(expectedIdeaCorePluginExplicitPackages + expectedJavaPluginExplicitPackages, packages)
      assertEquals(expectedIdeaCoreClasses + expectedJavaPluginClasses, allClassNames)
    }
  }

  @Test
  fun `dependencies are resolved`() {
    val pluginDependingOnJava = idePlugin("com.example.BetterJava") {
      depends("com.intellij.modules.java")
    }

    val ideVersion = IdeVersion.createIdeVersion("IU-243.12818.47")
    val ide = MockIde(ideVersion, ideRoot, bundledPlugins = listOf(ideaCorePlugin, javaPlugin, jsonPlugin))
    val resolverProvider = CachingPluginDependencyResolverProvider(ide)

    with(resolverProvider.getResolver(pluginDependingOnJava)) {
      assertEquals(expectedIdeaCorePluginPackages + expectedJavaPluginPackages, allPackages)
      assertEquals(expectedIdeaCoreClasses + expectedJavaPluginClasses, allClassNames)
    }
  }

  @Test
  fun `cyclic dependencies in IDE plugins that are retrieved from cache are handled`() {
    val alphaFiles = buildZipFile(temporaryFolder.newTemporaryFile("alpha/alpha.jar")) {
      dirs("com/example/alpha") {
        file("AlphaAction.class", createEmptyClass("com/example/alpha/AlphaAction"))
      }
    }
    val alphaPlugin = MockIdePlugin(
      pluginId = "com.example.Alpha",
      dependsList = dependency("com.example.Beta"),
      classpath = Classpath.of(listOf(alphaFiles))
    )

    val betaFiles = buildZipFile(temporaryFolder.newTemporaryFile("beta/beta.jar")) {
      dirs("com/example/beta") {
        file("BetaAction.class", createEmptyClass("com/example/beta/BetaAction"))
      }
    }
    val betaPlugin = MockIdePlugin(
      pluginId = "com.example.Beta",
      dependsList = dependency("com.example.Gamma"),
      classpath = Classpath.of(listOf(betaFiles))
    )

    val gammaFiles = buildZipFile(temporaryFolder.newTemporaryFile("gamma/gamma.jar")) {
      dirs("com/example/gamma") {
        file("GammaAction.class", createEmptyClass("com/example/gamma/GammaAction"))
      }
    }
    val gammaPlugin = MockIdePlugin(
      pluginId = "com.example.Gamma",
      dependsList = dependency("com.example.Alpha"),
      classpath = Classpath.of(listOf(gammaFiles))
    )

    val ideVersion = IdeVersion.createIdeVersion("IU-243.12818.47")
    val ide = MockIde(ideVersion, ideRoot, bundledPlugins = listOf(alphaPlugin, betaPlugin, gammaPlugin))

    val resolverProvider = CachingPluginDependencyResolverProvider(ide)

    with(resolverProvider.getResolver(alphaPlugin)) {
      val expectedAllPackages = setOf(
        "com",
        "com/example",
        "com/example/beta",
        "com/example/gamma",
      )
      assertEquals(expectedAllPackages, allPackages)

      val expectedPackages = setOf(
        "com/example/beta",
        "com/example/gamma",
      )
      assertEquals(expectedPackages, packages)
      // 'Alpha' plugin package should not be in the Alpha's transitive dependencies
      assertFalse(packages.contains("com/example/alpha"))

      val expectedClasses = binaryClassNames(
        "com/example/beta/BetaAction",
        "com/example/gamma/GammaAction",
      )
      assertEquals(expectedClasses, allClassNames)
      // 'Alpha' plugin classes should not be resolved in the dependencies
      assertFalse(packages.contains("com/example/alpha/AlphaAction"))
    }
  }

  @Test
  fun `plugin depends on itself`() {
    val alphaFiles = buildZipFile(temporaryFolder.newTemporaryFile("alpha/alpha.jar")) {
      dirs("com/example/alpha") {
        file("AlphaAction.class", createEmptyClass("com/example/alpha/AlphaAction"))
      }
    }
    val alphaPlugin = MockIdePlugin(
      pluginId = "com.example.Alpha",
      dependsList = dependency("com.example.Beta"),
      classpath = Classpath.of(listOf(alphaFiles))
    )

    val betaFiles = buildZipFile(temporaryFolder.newTemporaryFile("beta/beta.jar")) {
      dirs("com/example/beta") {
        file("BetaAction.class", createEmptyClass("com/example/beta/BetaAction"))
      }
    }
    val betaPlugin = MockIdePlugin(
      pluginId = "com.example.Beta",
      dependsList = dependency("com.example.Alpha"),
      classpath = Classpath.of(listOf(betaFiles))
    )

    val ideVersion = IdeVersion.createIdeVersion("IU-243.12818.47")
    val ide = MockIde(ideVersion, ideRoot, bundledPlugins = listOf(alphaPlugin, betaPlugin))

    val resolverProvider = CachingPluginDependencyResolverProvider(ide)
    val resolver = resolverProvider.getResolver(alphaPlugin)
    assertTrue(resolver is CachingPluginDependencyResolverProvider.DependencyTreeAwareResolver)
    resolver as CachingPluginDependencyResolverProvider.DependencyTreeAwareResolver
    with(resolver) {
      assertTrue(resolver.containsResolverName("com.example.Beta"))
      assertFalse(resolver.containsResolverName("com.example.Alpha"))
    }
  }

  @Test
  fun `transitive dependency resolver and plugin classpath resolver use different cache entries`() {
    val betaFiles = buildZipFile(temporaryFolder.newTemporaryFile("cache-collision/beta.jar")) {
      dirs("com/example/beta") {
        file("BetaAction.class", createEmptyClass("com/example/beta/BetaAction"))
      }
    }
    val betaPlugin = MockIdePlugin(
      pluginId = "com.example.Beta",
      pluginVersion = "1.0",
      originalFile = betaFiles,
      classpath = Classpath.of(listOf(betaFiles))
    )
    val alphaPlugin = idePlugin("com.example.Alpha") {
      depends("com.example.Beta")
    }
    val ideVersion = IdeVersion.createIdeVersion("IU-243.12818.47")
    val ide = MockIde(ideVersion, ideRoot, bundledPlugins = listOf(betaPlugin))
    val betaAction = "com/example/beta/BetaAction"

    val transitiveDependencyFirst = CachingPluginDependencyResolverProvider(ide)
    transitiveDependencyFirst.getResolver(betaPlugin)
    assertTrue(transitiveDependencyFirst.getResolver(alphaPlugin).containsClass(betaAction))

    val pluginClasspathFirst = CachingPluginDependencyResolverProvider(ide)
    pluginClasspathFirst.getResolver(alphaPlugin)
    val cachedBetaResolver = pluginClasspathFirst.getCachedPluginResolver(betaPlugin)
    assertNotNull(cachedBetaResolver)
    assertTrue(cachedBetaResolver!!.containsClass(betaAction))
    assertFalse(pluginClasspathFirst.getResolver(betaPlugin).containsClass(betaAction))
  }

  @Test
  fun `transitive dependency resolver cache distinguishes plugin versions`() {
    val pluginV1 = MockIdePlugin(
      pluginId = "com.example.Versioned",
      pluginVersion = "1.0",
      dependsList = dependency("com.intellij.modules.json")
    )
    val pluginV2 = MockIdePlugin(
      pluginId = "com.example.Versioned",
      pluginVersion = "2.0",
      dependsList = dependency("com.intellij.java")
    )
    val ideVersion = IdeVersion.createIdeVersion("IU-243.12818.47")
    val ide = MockIde(ideVersion, ideRoot, bundledPlugins = listOf(ideaCorePlugin, javaPlugin, jsonPlugin))
    val resolverProvider = CachingPluginDependencyResolverProvider(ide)

    val resolverV1 = resolverProvider.getResolver(pluginV1)
    assertTrue(resolverV1.containsClass("com/intellij/json/JsonNamesValidator"))
    assertFalse(resolverV1.containsClass("com/intellij/openapi/actionSystem/DataKeys"))

    val resolverV2 = resolverProvider.getResolver(pluginV2)
    assertTrue(resolverV2.containsClass("com/intellij/openapi/actionSystem/DataKeys"))
    assertFalse(resolverV2.containsClass("com/intellij/json/JsonNamesValidator"))
  }

  @Test
  fun `transitive dependency resolver cache distinguishes artifacts with same id and version`() {
    val artifactsDir = temporaryFolder.newFolder("same-version").toPath()
    val pluginArtifact1 = artifactsDir.resolve("plugin-1.zip")
    val pluginArtifact2 = artifactsDir.resolve("plugin-2.zip")
    val plugin1 = MockIdePlugin(
      pluginId = "com.example.SameVersion",
      pluginVersion = "1.0",
      originalFile = pluginArtifact1,
      dependsList = dependency("com.intellij.modules.json")
    )
    val plugin2 = MockIdePlugin(
      pluginId = "com.example.SameVersion",
      pluginVersion = "1.0",
      originalFile = pluginArtifact2,
      dependsList = dependency("com.intellij.java")
    )
    val ideVersion = IdeVersion.createIdeVersion("IU-243.12818.47")
    val ide = MockIde(ideVersion, ideRoot, bundledPlugins = listOf(ideaCorePlugin, javaPlugin, jsonPlugin))
    val resolverProvider = CachingPluginDependencyResolverProvider(ide)

    val resolver1 = resolverProvider.getResolver(plugin1)
    assertTrue(resolver1.containsClass("com/intellij/json/JsonNamesValidator"))
    assertFalse(resolver1.containsClass("com/intellij/openapi/actionSystem/DataKeys"))

    val resolver2 = resolverProvider.getResolver(plugin2)
    assertTrue(resolver2.containsClass("com/intellij/openapi/actionSystem/DataKeys"))
    assertFalse(resolver2.containsClass("com/intellij/json/JsonNamesValidator"))
  }

  @Test
  fun `plugin depends on JSON that is in the secondary cache, but not fully`() {
    val ideRoot = temporaryFolder.newFolder("idea-" + UUID.randomUUID().toString()).toPath()

    val jsonPluginDir = buildDirectory(ideRoot) {
      dir("plugins") {
        dir("json") {
          dir("lib") {
            zip("json.jar") {
              dirs("com/intellij/json") {
                file("JsonNamesValidator.class", createEmptyClass("com/intellij/json/JsonNamesValidator"))
              }
            }
            dir("modules") {
              zip("intellij.json.split.jar") {
                dir("com") {
                  dir("intellij") {
                    dir("json") {
                      file("JsonBundle.class", createEmptyClass("com/intellij/json/JsonBundle"))
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    val jsonPlugin = MockIdePlugin(
      pluginId = "com.intellij.modules.json",
      pluginName = "JSON",
      originalFile = jsonPluginDir,
      contentModuleDependencies = listOf(
        ContentModuleDependency("com.intellij.modules.lang", "jetbrains")
      ),
      pluginAliases = setOf("intellij.json", "intellij.json.split"),
      classpath = Classpath.of(listOf(ideRoot.resolve("plugins/json/lib/json.jar"), ideRoot.resolve("plugins/json/lib/modules/intellij.json.split.jar")))
    )

    val productInfo = ProductInfo(
      layout = listOf(
        LayoutComponent.Plugin("com.intellij.modules.json", classPaths = listOf("plugins/json/lib/json.jar")),
        LayoutComponent.ModuleV2("intellij.json", classPaths = listOf("plugins/json/lib/modules/intellij.json.jar")),
        LayoutComponent.ModuleV2("intellij.json.split", classPaths = listOf("plugins/json/lib/modules/intellij.json.split.jar"))
      ),
      name = UNKNOWN,
      version = UNKNOWN,
      versionSuffix = UNKNOWN,
      buildNumber = "243.12818.47",
      productCode = "IU",
      dataDirectoryName = UNKNOWN,
      svgIconPath = UNKNOWN,
      productVendor = UNKNOWN,
      launch = emptyList(),
      bundledPlugins = emptyList(),
      modules = emptyList()
    )
    val ide = MockProductInfoBasedIde(ideRoot, productInfo, bundledPlugins = listOf(jsonPlugin))
    val productInfoClassResolver = ProductInfoClassResolver.of(ide, IdeResolverConfiguration(readMode = Resolver.ReadMode.SIGNATURES))
    val resolverProvider = CachingPluginDependencyResolverProvider(ide, productInfoClassResolver)

    val alphaPlugin = idePlugin("com.example.Alpha") {
      depends("com.intellij.modules.json")
    }

    val pluginResolver = resolverProvider.getResolver(alphaPlugin)
    assertTrue(pluginResolver.containsClass("com/intellij/json/JsonNamesValidator"))
    assertTrue(pluginResolver.containsClass("com/intellij/json/JsonBundle"))
  }

  @Test
  fun `repeated main and module-only declarations resolve the same external classes without leaking local classes`() {
    val bundledClass = "com/example/bundled/BundledAction"
    val transitiveClass = "com/example/transitive/TransitiveAction"
    val transitivePlugin = buildClassPlugin("com.example.Transitive", transitiveClass)
    val bundledPlugin = buildClassPlugin("com.example.Bundled", bundledClass, dependency(transitivePlugin.pluginId!!))
    val ide = MockIde(
      IdeVersion.createIdeVersion("IU-243.12818.47"), ideRoot,
      bundledPlugins = listOf(ideaCorePlugin, bundledPlugin, transitivePlugin)
    )
    val contributor = DefaultDependencyContributor(includeContentModuleDependencies = true)
    val expectedDependencies = setOf(
      Dependency.Module(ideaCorePlugin, "com.intellij.modules.platform", isTransitive = false),
      Dependency.Plugin(bundledPlugin, isTransitive = false),
      Dependency.Plugin(transitivePlugin, isTransitive = true)
    )
    val expectedClasses = expectedIdeaCoreClasses.map { it.toString() }.toSet() + setOf(bundledClass, transitiveClass)
    val dependenciesByLayout = listOf(true, false).map { repeatMainDependencies ->
      val plugin = buildModularClassPlugin(repeatMainDependencies)
      val coreId = "com.example.Modular.core"
      val extraId = "com.example.Modular.extra"
      assertEquals("Both local modules must be parsed", setOf(coreId, extraId), plugin.modulesDescriptors.map { it.name }.toSet())
      assertEquals("The owner classpath must contain the root and both content module JARs",
        setOf("main.jar", "core.jar", "extra.jar"), plugin.classpath.entries.map { it.path.fileName.toString() }.toSet())
      assertTrue(plugin.dependsList.isEmpty())
      if (repeatMainDependencies) {
        assertEquals(listOf("com.example.Bundled"), plugin.pluginMainModuleDependencies.map { it.pluginId })
        assertEquals(listOf("com.intellij.modules.platform"), plugin.contentModuleDependencies.map { it.moduleName })
      } else {
        assertTrue(plugin.pluginMainModuleDependencies.isEmpty())
        assertTrue(plugin.contentModuleDependencies.isEmpty())
      }
      val coreDescriptor = plugin.modulesDescriptors.single { it.name == coreId }
      val extraDescriptor = plugin.modulesDescriptors.single { it.name == extraId }
      assertEquals(listOf("com.intellij.modules.platform"), coreDescriptor.declaredDependencies.map { it.id })
      assertEquals("Extra declares its sibling and bundled dependency", setOf(coreId, "com.example.Bundled"),
        extraDescriptor.declaredDependencies.map { it.id }.toSet())
      assertEquals(if (repeatMainDependencies) emptyList<String>() else listOf("com.intellij.modules.platform"),
        coreDescriptor.resolvedDependencies.map { it.id })
      assertEquals("Only main-descriptor duplicates are filtered",
        if (repeatMainDependencies) setOf(coreId) else setOf(coreId, "com.example.Bundled"),
        extraDescriptor.resolvedDependencies.map { it.id }.toSet())

      val resolverProvider = CachingPluginDependencyResolverProvider(
        ide, ideModulePredicate = HAS_COM_INTELLIJ_MODULE_PREFIX, dependenciesModifier = contributor
      )
      val resolver = resolverProvider.getResolver(plugin)
      assertTrue(resolver is CachingPluginDependencyResolverProvider.DependencyTreeAwareResolver)
      resolver as CachingPluginDependencyResolverProvider.DependencyTreeAwareResolver
      resolver.use {
        val externalResolverNames = setOf("com.intellij", "com.example.Bundled", "com.example.Transitive")
        assertTrue("Repeated declarations must not multiply external resolvers: $resolver",
          resolver.toString().startsWith("${plugin.pluginId} with 3 resolvers: "))
        externalResolverNames.forEach { name ->
          assertTrue("External resolver for $name is required", resolver.containsResolverName(name))
          assertTrue(resolverProvider.pluginResolverCacheContains(name))
        }
        listOf(plugin.pluginId!!, coreId, extraId).forEach { name ->
          assertFalse("Local ownership and sibling nodes must not create external resolvers for $name", resolver.containsResolverName(name))
          assertFalse(resolverProvider.pluginResolverCacheContains(name))
        }
        assertFalse("Platform alias must not create another resolver", resolver.containsResolverName("com.intellij.modules.platform"))
        listOf("com/intellij/openapi/graph/builder/actions/SelectionNodeModeAction", bundledClass, transitiveClass).forEach { className ->
          val result = resolver.resolveClass(className)
          assertTrue("External class $className must resolve, but got $result", result is ResolutionResult.Found)
          assertEquals(className, (result as ResolutionResult.Found).value.name)
        }
        assertEquals("Only external classes belong in the dependency resolver",
          expectedClasses, resolver.allClassNames.map { it.toString() }.toSet())
        listOf("com/example/modular/RootAction", "com/example/modular/CoreAction", "com/example/modular/ExtraAction").forEach { className ->
          assertFalse("Local class $className must not leak into external dependencies", resolver.containsClass(className))
          assertEquals(ResolutionResult.NotFound, resolver.resolveClass(className))
        }

        val resolution = resolver.dependencyTreeResolution
        assertTrue(resolution.missingDependencies.isEmpty())
        assertEquals("Exactly three flattened external dependencies are required", 3, resolution.transitiveDependencies.size)
        assertEquals("Flattening must preserve direct and transitive classification", expectedDependencies, resolution.transitiveDependencies.toSet())
        assertEquals("Both dependency APIs must expose the same flattened collection", expectedDependencies,
          DependencyTree(ide, HAS_COM_INTELLIJ_MODULE_PREFIX).getTransitiveDependencies(plugin, dependenciesModifier = contributor))

        val rootNode = NodeId.ofPlugin(plugin)
        val coreNode = NodeId(rootNode.pluginId, coreId)
        val extraNode = NodeId(rootNode.pluginId, extraId)
        val platformNode = Dependency.Module(ideaCorePlugin, "com.intellij.modules.platform").nodeId
        val bundledNode = NodeId.ofPlugin(bundledPlugin)
        val transitiveNode = NodeId.ofPlugin(transitivePlugin)
        val edges = linkedMapOf<NodeId, MutableSet<NodeId>>()
        resolution.forEach { from, to ->
          edges.getOrPut(requireNotNull(from.nodeId)) { linkedSetOf() } += requireNotNull(to.nodeId)
        }
        val rootDependencies = setOf(coreNode, extraNode) +
          if (repeatMainDependencies) setOf(platformNode, bundledNode) else emptySet()
        assertEquals("Local graph nodes and their declaration sources must survive flattening", mapOf(
          rootNode to rootDependencies,
          coreNode to setOf(platformNode),
          extraNode to setOf(coreNode, bundledNode),
          bundledNode to setOf(transitiveNode)
        ), edges)
        resolution.transitiveDependencies.toSet()
      }
    }
    assertEquals("Repeated-main and module-only layouts must have identical external dependencies and classification",
      dependenciesByLayout[0], dependenciesByLayout[1])
  }

  private fun buildClassPlugin(id: String, className: String, dependsList: List<DependsPluginDependency> = emptyList()): MockIdePlugin {
    val pluginFile = buildZipFile(temporaryFolder.newTemporaryFile("$id/plugin.jar")) {
      dirs(className.substringBeforeLast('/')) {
        file("${className.substringAfterLast('/')}.class", createEmptyClass(className))
      }
    }
    return MockIdePlugin(
      pluginId = id,
      originalFile = pluginFile,
      dependsList = dependsList,
      classpath = Classpath.of(listOf(pluginFile))
    )
  }

  private fun buildModularClassPlugin(repeatMainDependencies: Boolean): IdePlugin {
    val layout = if (repeatMainDependencies) "repeated-main" else "module-only"
    val mainDependencies = if (repeatMainDependencies) """
      <dependencies>
        <module name="com.intellij.modules.platform"/>
        <plugin id="com.example.Bundled"/>
      </dependencies>
    """.trimIndent() else ""
    val pluginRoot = buildDirectory(temporaryFolder.newFolder(layout).toPath()) {
      dir("lib") {
        zip("main.jar") {
          dir("META-INF") {
            file("plugin.xml", """
              <idea-plugin>
                <id>com.example.Modular</id>
                <name>Modular class resolution fixture</name>
                <version>1.0</version>
                <vendor>JetBrains</vendor>
                <description>A fixture verifying external class resolution for local content modules.</description>
                <idea-version since-build="243.0"/>
                $mainDependencies
                <content>
                  <module name="com.example.Modular.core" loading="required"/>
                  <module name="com.example.Modular.extra" loading="required"/>
                </content>
              </idea-plugin>
            """.trimIndent())
          }
          dirs("com/example/modular") {
            file("RootAction.class", createEmptyClass("com/example/modular/RootAction"))
          }
        }
        zip("core.jar") {
          file("com.example.Modular.core.xml", """
            <idea-plugin>
              <dependencies>
                <module name="com.intellij.modules.platform"/>
              </dependencies>
            </idea-plugin>
          """.trimIndent())
          dirs("com/example/modular") {
            file("CoreAction.class", createEmptyClass("com/example/modular/CoreAction"))
          }
        }
        zip("extra.jar") {
          file("com.example.Modular.extra.xml", """
            <idea-plugin>
              <dependencies>
                <module name="com.example.Modular.core"/>
                <plugin id="com.example.Bundled"/>
              </dependencies>
            </idea-plugin>
          """.trimIndent())
          dirs("com/example/modular") {
            file("ExtraAction.class", createEmptyClass("com/example/modular/ExtraAction"))
          }
        }
      }
    }
    val result = IdePluginManager.createManager().createPlugin(pluginRoot, validateDescriptor = true)
    assertTrue("Expected a successfully parsed $layout plugin, but got $result", result is PluginCreationSuccess)
    return (result as PluginCreationSuccess).plugin
  }

  private fun dependency(id: String): List<DependsPluginDependency> {
    return listOf(MandatoryV1Dependency(id))
  }

  private fun assertEquals(expected: Set<BinaryClassName>, actual: Set<BinaryClassName>): Boolean {
    if (expected == actual) return true
    if (expected.size != actual.size) return false
    for (expectedClass in expected) {
      for (actualClass in actual) {
        if (CharSequenceComparator.compare(expectedClass, actualClass) != 0) {
          return false
        }
      }
    }
    return true
  }
}
