package com.jetbrains.plugin.structure.ide.classes.resolver

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.createEmptyClass
import com.jetbrains.plugin.structure.base.utils.newTemporaryFile
import com.jetbrains.plugin.structure.intellij.plugin.Classpath
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.ModuleV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.MockIde
import com.jetbrains.plugin.structure.mocks.MockIdePlugin
import net.bytebuddy.ByteBuddy
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

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

  val expectedIdeaCorePluginPackages = setOf(
    "com" ,
    "com/intellij",
    "com/intellij/openapi",
    "com/intellij/openapi/graph",
    "com/intellij/openapi/graph/builder",
    "com/intellij/openapi/graph/builder/actions"
  )

  val expectedIdeaCorePluginExplicitPackages = setOf(
    "com/intellij/openapi/graph/builder/actions"
  )

  val expectedJavaPluginPackages = setOf(
    "com",
    "com/intellij",
    "com/intellij/openapi",
    "com/intellij/openapi/actionSystem",
  )

  val expectedJavaPluginExplicitPackages = setOf(
    "com/intellij/openapi/actionSystem",
  )

  private val expectedJsonPluginPackages = setOf(
    "com",
    "com/intellij",
    "com/intellij/json",
  )

  private val expectedJsonPluginExplicitPackages = setOf(
    "com/intellij/json",
  )

  private val expectedIdeaCoreClasses = setOf(
    "com/intellij/openapi/graph/builder/actions/SelectionNodeModeAction",
  )

  private val expectedJavaPluginClasses = setOf(
    "com/intellij/openapi/actionSystem/DataKeys",
  )

  private val expectedJsonPluginClasses = setOf(
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
    with(resolverProvider.getStats()) {
      assertNotNull(this); this!!
      /*
        "com.intellij.modules.platform" and "com.intellij.modules.lang" belong to the same module
       */
      assertEquals(1, hitCount())
      /*
        "com.example.somePlugin" (plugin itself), "com.intellij" and "com.intellij.modules.json" need to be created without cache
       */
      assertEquals(3, missCount())
    }

    with(resolver) {
      assertEquals(expectedIdeaCorePluginExplicitPackages + expectedJsonPluginExplicitPackages, packages)
      assertEquals(expectedIdeaCoreClasses + expectedJsonPluginClasses, allClasses)
    }

    val pluginDependingOnJavaResolver = resolverProvider.getResolver(pluginDependingOnJava)

    with(resolverProvider.getStats()) {
      assertNotNull(this); this!!
      /*
        One previous value. On top: 'Java' depends on 'Lang' that is already in cache, making a 2nd cache hit.
       */
      assertEquals(2, hitCount())
      /*
        Three previous values. On top, two values need to be created without the cache
        1) 'Java'
        2) 'com.example.BetterJava' (plugin itself)
       */
      assertEquals(5, missCount())
    }

    with(pluginDependingOnJavaResolver) {
      assertEquals(expectedIdeaCorePluginExplicitPackages + expectedJavaPluginExplicitPackages, packages)
      assertEquals(expectedIdeaCoreClasses + expectedJavaPluginClasses, allClasses)
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
      assertEquals(expectedIdeaCoreClasses + expectedJavaPluginClasses, allClasses)
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

      val expectedClasses = setOf(
        "com/example/beta/BetaAction",
        "com/example/gamma/GammaAction",
      )
      assertEquals(expectedClasses, allClasses)
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
    assertTrue(resolver is CachingPluginDependencyResolverProvider.ComponentNameAwareCompositeResolver)
    resolver as CachingPluginDependencyResolverProvider.ComponentNameAwareCompositeResolver
    with(resolver) {
      assertTrue(resolver.containsResolverName("com.example.Beta"))
      assertFalse(resolver.containsResolverName("com.example.Alpha"))
    }
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
}