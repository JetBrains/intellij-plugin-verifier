/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.resolution.DefaultClassResolverProvider
import com.jetbrains.pluginverifier.resolution.DefaultPluginDetailsBasedResolverProvider
import com.jetbrains.pluginverifier.resolution.PluginDetailsBasedResolverProvider
import com.jetbrains.pluginverifier.tests.BaseBytecodeTest
import com.jetbrains.pluginverifier.tests.mocks.MockDependencyFinder
import com.jetbrains.pluginverifier.tests.mocks.MockIdePlugin
import com.jetbrains.pluginverifier.tests.mocks.MockPackageFilter
import com.jetbrains.pluginverifier.tests.mocks.RuleBasedDependencyFinder
import com.jetbrains.pluginverifier.tests.mocks.RuleBasedDependencyFinder.Rule
import com.jetbrains.pluginverifier.tests.mocks.asm.publicClass
import com.jetbrains.pluginverifier.tests.mocks.getDetails
import org.intellij.lang.annotations.Language
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultClassResolverProviderTest : BaseBytecodeTest() {
  private val dependencyFinder = MockDependencyFinder()

  private val packageFilter = MockPackageFilter()

  private val plugin = MockIdePlugin(
    pluginId = "somePlugin",
    pluginVersion = "1.0"
  )

  private val pythonModuleDependency = PluginDependencyImpl("com.intellij.modules.python", false, true)
  private val platformModuleDependency = PluginDependencyImpl("com.intellij.modules.platform", false, true)

  @Test
  fun `legacy plugin without any dependencies resolves to IDEA Core plugin in Platform 192`() {
    val ide = buildIdeWithBundledPlugins()
    val ideDescriptor = IdeDescriptor.create(ide.idePath, defaultJdkPath = null, ideFileLock = null)

    val resolverProvider = DefaultClassResolverProvider(dependencyFinder, ideDescriptor, packageFilter)

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

    val resolverProvider = DefaultClassResolverProvider(dependencyFinder, ideDescriptor, packageFilter)

    val classResolver = resolverProvider.provide(plugin.getDetails())
    // class from app.jar from mock IDE
    assertTrue(classResolver.allResolver.containsClass("com/intellij/tasks/Task"))
  }

  @Test
  fun `plugin with a bundled dependency unavailable in the Platform 243, but downloaded by custom details resolver provider`() {
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
    val pluginDetailsResolverProvider = object : PluginDetailsBasedResolverProvider {
      override fun getPluginResolver(pluginDependency: PluginDetails): Resolver {
        return if (pluginDependency.idePlugin.pluginId == "com.intellij") {
          with(pluginDependency.pluginClassesLocations) {
            locationKeys
              .flatMap { getResolvers(it) }
              .let { resolvers -> CompositeResolver.create(resolvers) }
          }
        } else {
          defaultPluginDetailsBasedResolverProvider.getPluginResolver(pluginDependency)
        }
      }
    }

    val resolverProvider = DefaultClassResolverProvider(
      dependencyFinder,
      ideDescriptor,
      packageFilter,
      pluginDetailsBasedResolverProvider = pluginDetailsResolverProvider,
      downloadUnavailableBundledPlugins = true
    )

    val plugin = this.plugin.copy(dependencies = listOf(pythonModuleDependency))

    val classResolver = resolverProvider.provide(plugin.getDetails())
    // class from app.jar from mock IDE
    assertTrue(classResolver.allResolver.containsClass("com/intellij/tasks/Task"))
  }

  @Test
  fun `plugin with a bundled dependency unavailable in the Platform 243, but downloaded`() {
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
      downloadUnavailableBundledPlugins = true
    )

    val plugin = this.plugin.copy(dependencies = listOf(pythonModuleDependency))

    val classResolver = resolverProvider.provide(plugin.getDetails())
    // class from app.jar from mock IDE
    assertTrue(classResolver.allResolver.containsClass("com/intellij/tasks/Task"))
  }

  @Test
  fun `plugin with a bundled dependency unavailable in the Platform 223, but downloaded`() {
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

    val resolverProvider = DefaultClassResolverProvider(dependencyFinder, ideDescriptor, packageFilter)

    val plugin = plugin.copy(dependencies = listOf(pythonModuleDependency))

    val classResolver = resolverProvider.provide(plugin.getDetails())
    // class from app.jar from mock IDE
    assertTrue(classResolver.allResolver.containsClass("com/intellij/tasks/Task"))
  }

  private val mockPythonPlugin = MockIdePlugin(
    pluginId = "Pythonid",
    pluginVersion = "243.21565.193",
    dependencies = listOf(platformModuleDependency),
    definedModules = setOf("com.intellij.modules.python")
  )

  private val mockIdeaCorePlugin = MockIdePlugin(
    pluginId = "com.intellij",
    pluginVersion = "243.21565.193",
    definedModules = setOf("com.intellij.modules.platform")
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
}