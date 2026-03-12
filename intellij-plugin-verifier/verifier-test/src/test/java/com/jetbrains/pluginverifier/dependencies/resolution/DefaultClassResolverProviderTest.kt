/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.ide.classes.IdeResolverCreator
import com.jetbrains.plugin.structure.intellij.platform.ProductInfoParser
import com.jetbrains.plugin.structure.intellij.plugin.ModuleV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginArchiveManager
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.jdk.DefaultJdkDescriptorProvider
import com.jetbrains.pluginverifier.jdk.JdkDescriptorProvider
import com.jetbrains.pluginverifier.resolution.DefaultClassResolverProvider
import com.jetbrains.pluginverifier.resolution.DefaultPluginDetailsBasedResolverProvider
import com.jetbrains.pluginverifier.resolution.PluginDetailsBasedResolverProvider
import com.jetbrains.pluginverifier.tests.BaseBytecodeTest
import com.jetbrains.pluginverifier.tests.mocks.MockDependencyFinder
import com.jetbrains.pluginverifier.tests.mocks.MockIdePlugin
import com.jetbrains.pluginverifier.tests.mocks.MockPackageFilter
import com.jetbrains.pluginverifier.tests.mocks.MockProductInfoAwareIde
import com.jetbrains.pluginverifier.tests.mocks.RuleBasedDependencyFinder
import com.jetbrains.pluginverifier.tests.mocks.RuleBasedDependencyFinder.Rule
import com.jetbrains.pluginverifier.tests.mocks.asm.publicClass
import com.jetbrains.pluginverifier.tests.mocks.createPluginArchiveManager
import com.jetbrains.pluginverifier.tests.mocks.getDetails
import org.intellij.lang.annotations.Language
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.ZipOutputStream

class DefaultClassResolverProviderTest : BaseBytecodeTest() {
  private val dependencyFinder = MockDependencyFinder()

  private val packageFilter = MockPackageFilter()

  private val plugin = MockIdePlugin(
    pluginId = "somePlugin",
    pluginVersion = "1.0"
  )

  private val pythonModuleDependency = PluginDependencyImpl("com.intellij.modules.python", false, true)
  private val platformModuleDependency = PluginDependencyImpl("com.intellij.modules.platform", false, true)

  private lateinit var archiveManager: PluginArchiveManager

  @Before
  override fun setUp() {
    super.setUp()
    archiveManager = temporaryFolder.createPluginArchiveManager()
  }

  @Test
  fun `legacy plugin without any dependencies resolves to IDEA Core plugin in Platform 192`() {
    val ide = buildIdeWithBundledPlugins()
    val ideDescriptor = IdeDescriptor.create(ide.idePath, defaultJdkPath = null, ideFileLock = null)

    val resolverProvider = DefaultClassResolverProvider(dependencyFinder, ideDescriptor, packageFilter, archiveManager = archiveManager)

    val classResolver = resolverProvider.provide(plugin.getDetails())
    // class from app.jar from mock IDE
    assertTrue(classResolver.allResolver.containsClass("com/intellij/tasks/Task"))
  }

  @Test
  fun `legacy plugin without any dependencies resolves to IDEA Core plugin in Platform 243`() {
    val ide = buildIdeWithBundledPlugins(
      version = "IU-243.21565.193",
      productInfo = productInfoJsonIU243,
      hasModuleDescriptors = true
    )
    val ideDescriptor = IdeDescriptor.create(ide.idePath, defaultJdkPath = null, ideFileLock = null)

    val resolverProvider =
      DefaultClassResolverProvider(dependencyFinder, ideDescriptor, packageFilter, archiveManager = archiveManager)

    val classResolver = resolverProvider.provide(plugin.getDetails())
    // class from app.jar from mock IDE
    assertTrue(classResolver.allResolver.containsClass("com/intellij/tasks/Task"))
  }

  @Test
  fun `plugin declaring a dependency that is unavailable in the Platform 243, but downloaded by custom details resolver provider`() {
    val ide = buildIdeWithBundledPlugins(
      version = "IU-243.21565.193",
      productInfo = productInfoJsonIU243,
      hasModuleDescriptors = true
    )
    val ideDescriptor = IdeDescriptor.create(ide.idePath, defaultJdkPath = null, ideFileLock = null)

    val dependencyFinder = RuleBasedDependencyFinder.create(
      ide,
      Rule("com.intellij.modules.python", mockPythonPlugin),
      Rule(
        "com.intellij.modules.platform", mockIdeaCorePlugin, listOf(
          publicClass("com/intellij/tasks/Task")
        )
      ),
    )

    val defaultPluginDetailsBasedResolverProvider = DefaultPluginDetailsBasedResolverProvider()
    val pluginDetailsResolverProvider = PluginDetailsBasedResolverProvider { pluginDependency ->
      if (pluginDependency.idePlugin.pluginId == "com.intellij") {
        with(pluginDependency.pluginClassesLocations) {
          locationKeys
            .flatMap { getResolvers(it) }
            .let { resolvers -> CompositeResolver.create(resolvers) }
        }
      } else {
        defaultPluginDetailsBasedResolverProvider.getPluginResolver(pluginDependency)
      }
    }

    val resolverProvider = DefaultClassResolverProvider(
      dependencyFinder,
      ideDescriptor,
      packageFilter,
      pluginDetailsBasedResolverProvider = pluginDetailsResolverProvider,
      archiveManager = archiveManager
    )

    val plugin = this.plugin.copy(dependencies = listOf(pythonModuleDependency))

    val classResolver = resolverProvider.provide(plugin.getDetails())
    // class from app.jar from mock IDE
    assertTrue(classResolver.allResolver.containsClass("com/intellij/tasks/Task"))
  }

  @Test
  fun `plugin has a dependency that is unavailable in the Platform 243, but downloaded via legacy dependency tree resolution`() {
    val ide = buildIdeWithBundledPlugins(
      version = "IU-243.21565.193",
      productInfo = productInfoJsonIU243,
      hasModuleDescriptors = true
    )
    val ideDescriptor = IdeDescriptor.create(ide.idePath, defaultJdkPath = null, ideFileLock = null)

    val dependencyFinder = RuleBasedDependencyFinder.create(
      ide,
      Rule("com.intellij.modules.python", mockPythonPlugin),
      Rule(
        "com.intellij.modules.platform", mockIdeaCorePlugin, listOf(
          publicClass("com/intellij/tasks/Task")
        ), isBundledPlugin = true
      ),
    )

    val resolverProvider = DefaultClassResolverProvider(
      dependencyFinder,
      ideDescriptor,
      packageFilter,
      archiveManager = archiveManager
    )

    val plugin = this.plugin.copy(dependencies = listOf(pythonModuleDependency))

    val classResolver = resolverProvider.provide(plugin.getDetails())
    // class from app.jar from mock IDE
    assertTrue(classResolver.allResolver.containsClass("com/intellij/tasks/Task"))
  }

  @Test
  fun `plugin has a dependency that is unavailable in the Platform 243, but downloaded via dependency tree resolution`() {
    val ide = buildIdeWithBundledPlugins(
      version = "IU-243.21565.193",
      productInfo = productInfoJsonIU243,
      hasModuleDescriptors = true
    )
    val ideDescriptor = IdeDescriptor.create(ide.idePath, defaultJdkPath = null, ideFileLock = null)

    val dependencyFinder = RuleBasedDependencyFinder.create(
      ide,
      Rule("com.intellij.modules.python", mockPythonPlugin),
      Rule(
        "com.intellij.modules.platform", mockIdeaCorePlugin, listOf(
          publicClass("com/intellij/tasks/Task")
        ), isBundledPlugin = true
      ),
    )

    val resolverProvider = DefaultClassResolverProvider(
      dependencyFinder,
      ideDescriptor,
      packageFilter,
      archiveManager = archiveManager
    )

    val plugin = this.plugin.copy(dependencies = listOf(pythonModuleDependency))

    val classResolver = resolverProvider.provide(plugin.getDetails())
    // class from app.jar from mock IDE
    assertTrue(classResolver.allResolver.containsClass("com/intellij/tasks/Task"))
  }

  @Test
  fun `plugin has a dependency that is unavailable in the Platform 223, but downloaded`() {
    val ide = buildIdeWithBundledPlugins(version = "223.8836.41")
    val ideDescriptor = IdeDescriptor.create(ide.idePath, defaultJdkPath = null, ideFileLock = null)

    val dependencyFinder = RuleBasedDependencyFinder.create(
      ide,
      Rule("com.intellij.modules.python", mockPythonPlugin),
      Rule(
        "com.intellij.modules.platform", mockIdeaCorePlugin, listOf(
          publicClass("com/intellij/tasks/Task")
        ), isBundledPlugin = true
      ),
    )

    val resolverProvider = DefaultClassResolverProvider(
      dependencyFinder, ideDescriptor, packageFilter, archiveManager = archiveManager
    )

    val plugin = plugin.copy(dependencies = listOf(pythonModuleDependency))

    val classResolver = resolverProvider.provide(plugin.getDetails())
    // class from app.jar from mock IDE
    assertTrue(classResolver.allResolver.containsClass("com/intellij/tasks/Task"))
  }

  @Test
  fun `plugin is already present in the platform`() {
    val ideVersionString = "251.23774.435"

    val ideRoot = buildDirectory(temporaryFolder.newFolder("idea-${UUID.randomUUID()}").toPath()) {
      dir("lib") {
        file("app.jar", createEmptyZipByteArray())
        file("idea_rt.jar", createEmptyZipByteArray())
      }
      file("build.txt", "IU-$ideVersionString")
      file("product-info.json", productInfoJsonIU243)
    }

    val productInfoParser = ProductInfoParser()
    val jdkDescriptorProvider = DefaultJdkDescriptorProvider()

    val productInfo = productInfoParser.parse(ByteArrayInputStream(productInfoJsonIU243.toByteArray()), "Unit Test")
    val bundledPlugins = listOf(
      MockIdePlugin("com.intellij", pluginName = "IDEA Core", pluginVersion = ideVersionString)
    )
    val ide = MockProductInfoAwareIde(ideRoot, productInfo, bundledPlugins)
    val ideResolver = IdeResolverCreator.createIdeResolver(ide)
    val jdkResult = jdkDescriptorProvider.getJdkDescriptor(ide, defaultJdkPath = null)
    assertTrue(jdkResult is JdkDescriptorProvider.Result.Found)
    jdkResult as JdkDescriptorProvider.Result.Found

    val ideDescriptor = IdeDescriptor(ide, ideResolver, jdkResult.jdkDescriptor, ideFileLock = null)

    val mockPluginWithIdFromThePlatform = MockIdePlugin(
      "com.intellij", pluginVersion = ideVersionString,
      dependencies = listOf(ModuleV2Dependency("com.example.SomeModuleV2Dependency", isOptional = true))
    )

    val emptyDependencyFinder = RuleBasedDependencyFinder.create(ide)
    val resolverProvider =
      DefaultClassResolverProvider(emptyDependencyFinder, ideDescriptor, packageFilter, archiveManager = archiveManager)

    val classResolver = resolverProvider.provide(mockPluginWithIdFromThePlatform.getDetails())
    with(classResolver.dependenciesGraph) {
      assertEquals(1, vertices.size)
      val vertex = vertices.first()
      assertEquals("com.intellij", vertex.pluginId)
      assertEquals("251.23774.435", vertex.version)

      with(missingDependencies) {
        assertEquals(1, size)
        val missingDependencyDeclarer = missingDependencies.keys.first()
        assertEquals("com.intellij", missingDependencyDeclarer.pluginId)
        assertEquals("251.23774.435", missingDependencyDeclarer.version)

        val missingComIntellijDependencies = missingDependencies[missingDependencyDeclarer] ?: emptySet()
        assertEquals(1, missingComIntellijDependencies.size)
        val missingComIntelliJDependency = missingComIntellijDependencies.first()
        assertEquals("com.example.SomeModuleV2Dependency", missingComIntelliJDependency.dependency.id)
      }
    }
  }

  private fun createEmptyZipByteArray(): ByteArray {
    val buffer = ByteArrayOutputStream()
    ZipOutputStream(buffer).use {}
    return buffer.toByteArray()
  }

  private val mockPythonPlugin = MockIdePlugin(
    pluginId = "Pythonid",
    pluginVersion = "243.21565.193",
    dependencies = listOf(platformModuleDependency),
    pluginAliases = setOf("com.intellij.modules.python")
  )

  private val mockIdeaCorePlugin = MockIdePlugin(
    pluginId = "com.intellij",
    pluginVersion = "243.21565.193",
    pluginAliases = setOf("com.intellij.modules.platform")
  )

  @Language("JSON")
  private val productInfoJsonIU243 = """
    {
      "name": "IntelliJ IDEA",
      "version": "2024.3",
      "buildNumber": "243.21565.193",
      "productCode": "IU",
      "envVarBaseName": "IDEA",
      "dataDirectoryName": "IntelliJIdea2024.3",
      "svgIconPath": "../bin/idea.svg",
      "productVendor": "JetBrains",
      "launch": [
        {
          "os": "macOS",
          "arch": "aarch64",
          "launcherPath": "../MacOS/idea",
          "javaExecutablePath": "../jbr/Contents/Home/bin/java",
          "vmOptionsFilePath": "../bin/idea.vmoptions",
          "bootClassPathJarNames": [
            "app.jar",
            "idea_rt.jar"
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
            "lib/app.jar",
            "lib/idea_rt.jar"
          ]
        }      
      ]
    } 
  """.trimIndent()

  @After
  fun tearDown() {
    archiveManager.close()
  }
}