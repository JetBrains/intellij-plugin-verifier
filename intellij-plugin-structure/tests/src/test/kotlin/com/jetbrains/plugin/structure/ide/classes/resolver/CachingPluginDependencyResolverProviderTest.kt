package com.jetbrains.plugin.structure.ide.classes.resolver

import com.jetbrains.plugin.structure.base.BinaryClassName
import com.jetbrains.plugin.structure.base.utils.CharSequenceComparator
import com.jetbrains.plugin.structure.base.utils.binaryClassNames
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.createEmptyClass
import com.jetbrains.plugin.structure.base.utils.newTemporaryFile
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.classes.IdeResolverConfiguration
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.plugin.Classpath
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.ModuleV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.MockIde
import com.jetbrains.plugin.structure.mocks.MockIdePlugin
import com.jetbrains.plugin.structure.mocks.MockProductInfoBasedIde
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
      definedModules = setOf(
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
      definedModules = setOf(
        "com.intellij.modules.java",
      ),
      dependencies = listOf(
        ModuleV2Dependency("com.intellij.modules.lang")
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
      dependencies = listOf(
        ModuleV2Dependency("com.intellij.modules.lang")
      ),
      classpath = Classpath.of(listOf(jsonPluginFile))
    )
  }

  @Test
  fun `cache is used properly`() {
    val ideVersion = IdeVersion.createIdeVersion("IU-243.12818.47")

    val plugin = MockIdePlugin(
      pluginId = "com.example.somePlugin",
      dependencies = listOf(
        PluginDependencyImpl(/* id = */ "com.intellij.modules.platform",
          /* isOptional = */ false,
          /* isModule = */ true
        ),
        PluginDependencyImpl(/* id = */ "com.intellij.modules.json",
          /* isOptional = */ false,
          /* isModule = */ true
        ),
      )
    )
    val pluginDependingOnJava = MockIdePlugin(
      pluginId = "com.example.BetterJava",
      dependencies = listOf(
        PluginDependencyImpl(/* id = */ "com.intellij.modules.java",
          /* isOptional = */ false,
          /* isModule = */ true
        )
      )
    )

    val ide = MockIde(ideVersion, ideRoot, bundledPlugins = listOf(ideaCorePlugin, javaPlugin, jsonPlugin))

    val resolverProvider = CachingPluginDependencyResolverProvider(ide)
    val resolver = resolverProvider.getResolver(plugin)
    val corePluginCacheHit = 0L
    with(resolverProvider.getStats()) {
      assertNotNull(this); this!!
      /*
        "com.intellij.modules.platform" and "com.intellij.modules.lang" belong to the same module.
        Cache was not even hit, since the resolver for the second invocation is already in the list
        of modules since the first invocation
       */
      assertEquals(corePluginCacheHit, hitCount())
      /*
        1) "com.example.somePlugin" (plugin itself),
        2) "com.intellij"
        3) "com.intellij" sole 'classpath' entry
        4) "com.intellij.modules.json"
        5) "com.intellij.modules.json" sole 'classpath' entry
        The modules of "com.intellij" are not considered to be cache misses as they are conflated with the
        "com.intellij" plugin.
        - "com.intellij.modules.platform" as module of "com.intellij"
        - "com.intellij.modules.lang" as a module of "com.intellij"
       */
      assertEquals(5,  missCount())
    }
    listOf(
      "com.example.somePlugin",
      "com.intellij",
      "com.intellij/product.jar",
      "com.intellij.modules.json",
      "com.intellij.modules.json/json.jar",
      "com.intellij.modules.platform",
      "com.intellij.modules.lang")
      .forEach {
        assertTrue("Resolver must cache $it", resolverProvider.contains(it))
      }

    with(resolver) {
      assertEquals(expectedIdeaCorePluginExplicitPackages + expectedJsonPluginExplicitPackages, packages)
      assertEquals(expectedIdeaCoreClasses + expectedJsonPluginClasses, allClassNames)
    }

    val pluginDependingOnJavaResolver = resolverProvider.getResolver(pluginDependingOnJava)

    with(resolverProvider.getStats()) {
      assertNotNull(this); this!!
      /*
        All seven (7) modules and plugins are in the cache. Add one for Java itself.
       */
      assertEquals(8, hitCount())
      /*
        All seven (7) modules and plugins are in the cache. Add one for Java itself.
       */
      assertEquals(8, missCount())
    }

    with(pluginDependingOnJavaResolver) {
      assertEquals(expectedIdeaCorePluginExplicitPackages + expectedJavaPluginExplicitPackages, packages)
      assertEquals(expectedIdeaCoreClasses + expectedJavaPluginClasses, allClassNames)
    }
  }

  @Test
  fun `dependencies are resolved`() {
    val pluginDependingOnJava = MockIdePlugin(
      pluginId = "com.example.BetterJava",
      dependencies = listOf(
        PluginDependencyImpl(/* id = */ "com.intellij.modules.java",
          /* isOptional = */ false,
          /* isModule = */ true
        )
      )
    )

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
      dependencies = dependency("com.example.Beta"),
      classpath = Classpath.of(listOf(alphaFiles))
    )

    val betaFiles = buildZipFile(temporaryFolder.newTemporaryFile("beta/beta.jar")) {
      dirs("com/example/beta") {
        file("BetaAction.class", createEmptyClass("com/example/beta/BetaAction"))
      }
    }
    val betaPlugin = MockIdePlugin(
      pluginId = "com.example.Beta",
      dependencies = dependency("com.example.Gamma"),
      classpath = Classpath.of(listOf(betaFiles))
    )

    val gammaFiles = buildZipFile(temporaryFolder.newTemporaryFile("gamma/gamma.jar")) {
      dirs("com/example/gamma") {
        file("GammaAction.class", createEmptyClass("com/example/gamma/GammaAction"))
      }
    }
    val gammaPlugin = MockIdePlugin(
      pluginId = "com.example.Gamma",
      dependencies = dependency("com.example.Alpha"),
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
      dependencies = dependency("com.example.Beta"),
      classpath = Classpath.of(listOf(alphaFiles))
    )

    val betaFiles = buildZipFile(temporaryFolder.newTemporaryFile("beta/beta.jar")) {
      dirs("com/example/beta") {
        file("BetaAction.class", createEmptyClass("com/example/beta/BetaAction"))
      }
    }
    val betaPlugin = MockIdePlugin(
      pluginId = "com.example.Beta",
      dependencies = dependency("com.example.Alpha"),
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
      dependencies = listOf(
        ModuleV2Dependency("com.intellij.modules.lang")
      ),
      definedModules = setOf("intellij.json", "intellij.json.split"),
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

    val alphaPlugin = MockIdePlugin(
      pluginId = "com.example.Alpha",
      dependencies = dependency("com.intellij.modules.json"),
    )

    val pluginResolver = resolverProvider.getResolver(alphaPlugin)
    assertTrue(pluginResolver.containsClass("com/intellij/json/JsonNamesValidator"))
    assertTrue(pluginResolver.containsClass("com/intellij/json/JsonBundle"))
  }

  private fun dependency(id: String): List<PluginDependency> {
    return listOf(PluginDependencyImpl(id,
      /* isOptional = */ false,
      /* isModule = */ false
    ))
  }

  private fun dependency(vararg pluginIdentifiers: String): List<PluginDependency> {
    return pluginIdentifiers.map { id ->
      PluginDependencyImpl(id,
        /* isOptional = */ false,
        /* isModule = */ false
      )
    }
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