package com.jetbrains.plugin.structure.ide.classes.resolver

import com.jetbrains.plugin.structure.base.utils.createEmptyClass
import com.jetbrains.plugin.structure.base.utils.createParentDirs
import com.jetbrains.plugin.structure.base.utils.writeText
import com.jetbrains.plugin.structure.classes.resolvers.LazyCompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.classes.resolvers.Resolver.ReadMode.FULL
import com.jetbrains.plugin.structure.ide.classes.IdeResolverConfiguration
import com.jetbrains.plugin.structure.intellij.platform.ProductInfoParser
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.MockIde
import com.jetbrains.plugin.structure.mocks.MockIdePlugin
import com.jetbrains.plugin.structure.mocks.MockProductInfoAwareIde
import org.intellij.lang.annotations.Language
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.zip.ZipOutputStream

private const val IDEA_ULTIMATE_2024_2 = "IU-242.18071.24"

class ProductInfoClassResolverTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var ideRoot: Path

  @Before
  fun setUp() {
    with(temporaryFolder.newFolder("idea")) {
      ideRoot = toPath()
      val productInfoJsonPath = ideRoot.resolve("product-info.json")
      copyResource("/ide/productInfo/product-info_mini.json", productInfoJsonPath)

      ideRoot.resolve("build.txt").writeText(IDEA_ULTIMATE_2024_2)

      ideRoot.createEmptyIdeFiles()
      createEmptyJarClassFiles()
    }
  }

  @Test
  fun `resolver is created from an IDE instance`() {
    val ide = MockIde(IdeVersion.createIdeVersion(IDEA_ULTIMATE_2024_2), ideRoot, bundledPlugins = listOf(corePlugin()))
    val resolver = ProductInfoClassResolver.of(ide)

    assertTrue(resolver.packages.isNotEmpty())

    with(resolver.allPackages) {
      assertEquals(5, size)
      assertTrue(contains("com"))
      assertTrue(contains("com/intellij/execution/process"))
      assertTrue(contains("com/intellij/execution/process/elevation"))
      assertTrue(contains("com/intellij"))
      assertTrue(contains("com/intellij/execution"))
    }

    with(resolver.packages) {
      assertEquals(setOf("com/intellij/execution/process/elevation"), this)
    }

    with(resolver.layoutComponentNames) {
      assertEquals(7, size)
      assertEquals(
        setOf(
          "Git4Idea",
          "com.intellij",
          "com.intellij.modules.all",
          "com.intellij.platform.ide.provisioner",
          "intellij.copyright.vcs",
          "intellij.execution.process.elevation",
          "intellij.java.featuresTrainer"
        ), this.toSet()
      )
    }
    with(resolver.bootClasspathResolver) {
      assertNotNull(this)
      assertTrue(this is LazyCompositeResolver)
    }

    val elevationLogger = resolver.resolveClass("com/intellij/execution/process/elevation/ElevationLogger")
    assertTrue(elevationLogger is ResolutionResult.Found)

    val elevationResolver = resolver.getLayoutComponentResolver("intellij.execution.process.elevation")
    assertNotNull(elevationResolver)
    elevationResolver!!

    val ideProvisioner = resolver.getLayoutComponentResolver("com.intellij.platform.ide.provisioner")
    assertNotNull(ideProvisioner)
    ideProvisioner!!
  }

  @Test
  fun `resolver is a plugin resolver provider`() {
    val ide = MockIde(IdeVersion.createIdeVersion(IDEA_ULTIMATE_2024_2), ideRoot, bundledPlugins = listOf(corePlugin()))
    val resolver = ProductInfoClassResolver.of(ide)

    val expectedPlugins = listOf(
      "com.intellij",
      "Git4Idea",
      "com.intellij",
      "intellij.execution.process.elevation",
      "intellij.java.featuresTrainer"
    )
    val actual = expectedPlugins.mapNotNull {
      val result = resolver.contains(it)
      if (!result) it else null
    }
    if (actual.isNotEmpty()) {
      fail("The following resolvers were not found: $actual")
    }

    val resolvers = expectedPlugins.mapNotNull {
      resolver.getResolver(MockIdePlugin(pluginId = it))
    }
    assertEquals(expectedPlugins.size, resolvers.size)
  }

  @Test
  fun `resolver supports 242+ IDE`() {
    assertTrue(ProductInfoClassResolver.supports(ideRoot))
  }

  @Test
  fun `resolver does not parse ProductInfo-aware IDE`() {
    val ideRoot = temporaryFolder.newFolder("idea-${UUID.randomUUID()}").toPath()

    val productInfoParser = ProductInfoParser()
    val productInfo = productInfoParser.parse(ByteArrayInputStream(productInfoJsonIU251WithTwoLayoutComponentsAndEmptyBootClasspath.toByteArray()), "Unit Test")

    val bundledPlugins = listOf(
      MockIdePlugin("com.intellij", pluginName = "IDEA Core")
    )

    val ide = MockProductInfoAwareIde(ideRoot, productInfo, bundledPlugins)
    val resolver = ProductInfoClassResolver.of(ide)
    assertEquals(listOf("com.intellij", "com.jetbrains.sh"), resolver.layoutComponentNames)
  }

  @Test
  fun `boot resolvers are resolved`() {
    val ideRoot = temporaryFolder.newFolder("idea-${UUID.randomUUID()}").toPath()
    ideRoot.createEmptyIdeFiles()

    val productInfoParser = ProductInfoParser()
    val productInfo = productInfoParser.parse(ByteArrayInputStream(productInfoJsonWithBootClassPathAndASingleLayoutComponent.toByteArray()), "Unit Test")
    val bundledPlugins = listOf(
      MockIdePlugin("com.intellij", pluginName = "IDEA Core")
    )

    val ide = MockProductInfoAwareIde(ideRoot, productInfo, bundledPlugins)

    val resolverCfg = IdeResolverConfiguration(readMode = FULL, isCollectingStats = true)
    val resolver = ProductInfoClassResolver.of(ide, resolverCfg)
    assertEquals(listOf("com.intellij"), resolver.layoutComponentNames)

    // Expecting all entries to be cache hits, as they are subset of "com.intellij" layout component
    val expectedCacheHits = productInfo.launches.first().bootClassPathJarNames.size
    assertEquals(expectedCacheHits, resolver.stats.size)
  }

  private fun copyResource(resource: String, targetFile: Path) {
    val url: URL = this::class.java.getResource(resource) ?: throw AssertionError("Resource '$resource' not found")
    url.openStream().use {
      Files.copy(it, targetFile)
    }
  }

  private fun Path.createEmptyIdeFiles() {
    ideFiles.flatMap { (_, files) -> files }
      .map { file ->
        resolve(file).apply {
          createParentDirs()
        }
      }.forEach {
        it.createEmptyZip()
      }
  }

  private fun createEmptyJarClassFiles() {
    ideClasses.forEach { (jarFile, classFileNames) ->
      val jarPath: Path = ideRoot.resolve(jarFile)
      JarOutputStream(Files.newOutputStream(jarPath)).use { jarOut ->
        classFileNames.forEach { classFileName ->
          val jarEntry = JarEntry(classFileName)

          jarOut.putNextEntry(jarEntry)
          jarOut.write(createEmptyClass(classFileName.removeSuffix(".class")))
          jarOut.closeEntry()
        }
      }
    }
  }

  private fun Path.createEmptyZip() {
    ZipOutputStream(Files.newOutputStream(this)).use {}
  }

  private fun corePlugin(): IdePlugin = MockIdePlugin(
    pluginId = "com.intellij",
    definedModules = setOf("com.intellij.platform.ide.provisioner")
  )

  private val ideFiles = mapOf<PluginId, List<String>>(
    // boot classpath, generally mapped to "bootClassPathJarNames" from product-info.json
    "IDEA Core" to listOf(
      "lib/platform-loader.jar",
      "lib/util-8.jar",
      "lib/util.jar",
      "lib/app-client.jar",
      "lib/util_rt.jar",
      "lib/product.jar",
      "lib/opentelemetry.jar",
      "lib/app.jar",
      "lib/product-client.jar",
      "lib/lib-client.jar",
      "lib/stats.jar",
      "lib/jps-model.jar",
      "lib/external-system-rt.jar",
      "lib/rd.jar",
      "lib/bouncy-castle.jar",
      "lib/protobuf.jar",
      "lib/intellij-test-discovery.jar",
      "lib/forms_rt.jar",
      "lib/kotlinx-coroutines-slf4j-1.8.0-intellij.jar",
      "lib/lib.jar",
      "lib/externalProcess-rt.jar",
      "lib/groovy.jar",
      "lib/annotations.jar",
      "lib/idea_rt.jar",
      "lib/jsch-agent.jar",
      "lib/junit4.jar",
      "lib/nio-fs.jar",
      "lib/trove.jar"
    ),

    "Git4Idea" to listOf(
      "plugins/vcs-git/lib/vcs-git.jar",
      "plugins/vcs-git/lib/git4idea-rt.jar"
    ),
    "com.intellij" to listOf(
      "lib/platform-loader.jar",
      "lib/util-8.jar",
      "lib/util.jar",
      "lib/util_rt.jar",
      "lib/product.jar",
      "lib/opentelemetry.jar",
      "lib/app.jar",
      "lib/stats.jar",
      "lib/jps-model.jar",
      "lib/external-system-rt.jar",
      "lib/rd.jar",
      "lib/bouncy-castle.jar",
      "lib/protobuf.jar",
      "lib/intellij-test-discovery.jar",
      "lib/forms_rt.jar",
      "lib/kotlinx-coroutines-slf4j-1.8.0-intellij.jar",
      "lib/lib.jar",
      "lib/externalProcess-rt.jar",
      "lib/groovy.jar",
      "lib/annotations.jar",
      "lib/idea_rt.jar",
      "lib/intellij-coverage-agent-1.0.750.jar",
      "lib/jsch-agent.jar",
      "lib/junit.jar",
      "lib/junit4.jar",
      "lib/nio-fs.jar",
      "lib/testFramework.jar",
      "lib/trove.jar"
    ),
    "intellij.execution.process.elevation" to listOf(
      "lib/modules/intellij.execution.process.elevation.jar"
    ),
    "intellij.java.featuresTrainer" to listOf(
      "plugins/java/lib/modules/intellij.java.featuresTrainer.jar"
    )
  )

  private val ideClasses = mapOf(
    "lib/modules/intellij.execution.process.elevation.jar" to listOf(
      "com/intellij/execution/process/elevation/ElevationLogger.class",
    )
  )

  @Language("JSON")
  private val productInfoJsonIU251WithTwoLayoutComponentsAndEmptyBootClasspath = """
    {
      "name": "IntelliJ IDEA",
      "version": "2025.1",
      "buildNumber": "251.23774.435",
      "productCode": "IU",
      "envVarBaseName": "IDEA",
      "dataDirectoryName": "IntelliJIdea2025.1",
      "svgIconPath": "../bin/idea.svg",
      "productVendor": "JetBrains",
      "launch": [
        {
          "os": "macOS",
          "arch": "aarch64",
          "launcherPath": "../MacOS/idea",
          "javaExecutablePath": "../jbr/Contents/Home/bin/java",
          "vmOptionsFilePath": "../bin/idea.vmoptions",
          "bootClassPathJarNames": []      
        }
      ],        
      "bundledPlugins": [],
      "modules": [],
      "layout": [
        {
          "name": "com.intellij",
          "kind": "plugin",
          "classPath": []
        },         
        {
          "name": "com.jetbrains.sh",
          "kind": "plugin",
          "classPath": []
        }
      ]        
    } 
  """.trimIndent()

  @Language("JSON")
  private val productInfoJsonWithBootClassPathAndASingleLayoutComponent = """
    {
      "name": "IntelliJ IDEA",
      "version": "2025.1",
      "buildNumber": "251.23774.435",
      "productCode": "IU",
      "envVarBaseName": "IDEA",
      "dataDirectoryName": "IntelliJIdea2025.1",
      "svgIconPath": "bin/idea.svg",
      "productVendor": "JetBrains",
      "launch": [
        {
          "os": "Linux",
          "arch": "amd64",
          "launcherPath": "bin/idea",
          "javaExecutablePath": "jbr/bin/java",
          "vmOptionsFilePath": "bin/idea64.vmoptions",
          "startupWmClass": "jetbrains-idea",
          "bootClassPathJarNames": [
            "platform-loader.jar",
            "util-8.jar",
            "util.jar",
            "app-client.jar",
            "util_rt.jar",
            "product.jar",
            "lib-client.jar",
            "trove.jar",
            "app.jar",
            "opentelemetry.jar",
            "jps-model.jar",
            "stats.jar",
            "rd.jar",
            "external-system-rt.jar",
            "protobuf.jar",
            "bouncy-castle.jar",
            "intellij-test-discovery.jar",
            "forms_rt.jar",
            "lib.jar",
            "externalProcess-rt.jar",
            "groovy.jar",
            "annotations.jar",
            "idea_rt.jar",
            "jsch-agent.jar",
            "kotlinx-coroutines-slf4j-1.8.0-intellij.jar",
            "product-client.jar"
          ]
        }
      ],
      "bundledPlugins": [],
      "modules": [],
      "layout": [
        {
          "name": "com.intellij",
          "kind": "plugin",
          "classPath": [
            "lib/platform-loader.jar",
            "lib/util-8.jar",
            "lib/util.jar",
            "lib/app-client.jar",
            "lib/util_rt.jar",
            "lib/product.jar",
            "lib/lib-client.jar",
            "lib/trove.jar",
            "lib/app.jar",
            "lib/opentelemetry.jar",
            "lib/jps-model.jar",
            "lib/stats.jar",
            "lib/rd.jar",
            "lib/external-system-rt.jar",
            "lib/protobuf.jar",
            "lib/bouncy-castle.jar",
            "lib/nio-fs.jar",
            "lib/intellij-test-discovery.jar",
            "lib/forms_rt.jar",
            "lib/lib.jar",
            "lib/externalProcess-rt.jar",
            "lib/groovy.jar",
            "lib/annotations.jar",
            "lib/idea_rt.jar",
            "lib/jsch-agent.jar",
            "lib/kotlinx-coroutines-slf4j-1.8.0-intellij.jar",
            "lib/product-client.jar",
            "lib/testFramework.jar"
          ]
        }
      ]
    }    
  """.trimIndent()
}

private typealias PluginId = String

