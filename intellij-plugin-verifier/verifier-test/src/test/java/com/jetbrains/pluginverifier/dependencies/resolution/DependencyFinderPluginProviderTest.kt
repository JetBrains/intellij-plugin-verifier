/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.pluginverifier.tests.BaseBytecodeTest
import com.jetbrains.pluginverifier.tests.mocks.MockIdePlugin
import com.jetbrains.pluginverifier.tests.mocks.RuleBasedDependencyFinder
import com.jetbrains.pluginverifier.tests.mocks.RuleBasedDependencyFinder.Rule
import org.intellij.lang.annotations.Language
import org.junit.Assert.assertNotNull
import org.junit.Test

class DependencyFinderPluginProviderTest : BaseBytecodeTest() {
  @Test
  fun `plugin is resolved as a dependency`() {
    val ide = buildIdeWithBundledPlugins(
      version = "IU-243.21565.193",
      productInfo = productInfoJsonIU243,
      hasModuleDescriptors = true
    )
    val dependencyFinder = RuleBasedDependencyFinder.create(
      ide,
      Rule("com.intellij.modules.python", mockPythonPlugin),
    )

    val pluginProvider = DependencyFinderPluginProvider(dependencyFinder, ide)
    val plugin = pluginProvider.findPluginById("com.intellij.modules.python")
    assertNotNull(plugin)
  }

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

  private val mockPythonPlugin = MockIdePlugin(
    pluginId = "Pythonid",
    pluginVersion = "243.21565.193",
    definedModules = setOf("com.intellij.modules.python")
  )
}