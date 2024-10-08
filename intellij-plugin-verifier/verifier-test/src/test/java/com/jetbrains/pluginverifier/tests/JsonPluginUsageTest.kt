/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.pluginverifier.results.problems.PackageNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.UndeclaredPluginDependencyProblem
import com.jetbrains.pluginverifier.tests.bytecode.Dumps
import com.jetbrains.pluginverifier.tests.mocks.IdeaPluginSpec
import com.jetbrains.pluginverifier.tests.mocks.bundledPlugin
import com.jetbrains.pluginverifier.tests.mocks.ideaPlugin
import com.jetbrains.pluginverifier.tests.mocks.withRootElement
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private const val JSON_PLUGIN_ID = "com.intellij.modules.json"

class JsonPluginUsageTest : BaseBytecodeTest() {
  private val pluginSpec = IdeaPluginSpec("com.intellij.plugin", "JetBrains s.r.o.")

  private val jsonPlugin
    get() = bundledPlugin {
      id = JSON_PLUGIN_ID
      artifactName = "json"
      descriptorContent = ideaPlugin(
        pluginId = JSON_PLUGIN_ID,
        pluginName = "JSON",
        vendor = "JetBrains s.r.o."
      ).withRootElement()
      classContentBuilder = {
        dirs("com/intellij/json") {
          file("JsonParserDefinition.class", JsonParserDefinition())
        }
      }
    }

  @Test
  fun `plugin uses JSON classes but they are not available in the IDE`() {
    assertVerified {
      ide = buildIdeWithBundledPlugins {}
      plugin = prepareUsage(pluginSpec, "JsonPluginUsage", Dumps.JsonPluginUsage())
      kotlin = false
    }.run {
      with(compatibilityProblems) {
        assertEquals(1, size)
        assertContains(this, PackageNotFoundProblem::class)
      }
    }
  }

  @Test
  fun `plugin uses JSON classes, JSON plugin is declared, but without any classes`() {
    val targetIde = buildIdeWithBundledPlugins(listOf(jsonPlugin))
    assertEquals(2, targetIde.bundledPlugins.size)

    assertVerified {
      ide = targetIde
      plugin = prepareUsage(pluginSpec, "JsonPluginUsage", Dumps.JsonPluginUsage())
      kotlin = false
    }.run {
      with(compatibilityProblems) {
        assertContains(this, PackageNotFoundProblem::class)
      }
    }
  }

  @Test
  fun `plugin uses JSON classes, JSON plugin is declared and includes classes`() {
    val targetIde = buildIdeWithBundledPlugins(bundledCorePlugins = listOf(jsonPlugin))
    assertEquals(2, targetIde.bundledPlugins.size)

    assertVerified {
      ide = targetIde
      plugin = prepareUsage(pluginSpec, "JsonPluginUsage", Dumps.JsonPluginUsage())
      kotlin = false
    }.run {
      with(compatibilityProblems) {
        assertEquals(0, size)
      }
    }
  }

  @Test
  fun `plugin uses JSON classes in the 242 IDE, but the JSON plugin dependency is not declared`() {
    val targetIde = buildIdeWithBundledPlugins(
      bundledPlugins = listOf(jsonPlugin),
      productInfo = onlyJsonPluginProductInfoValue,
      version = "IC-243.16128",
      hasModuleDescriptors = true
    )
    assertEquals(2, targetIde.bundledPlugins.size)
    targetIde.assertHasBundledPluginWithPath(Paths.get("plugins/json/lib/json.jar"))

    assertVerified {
      ide = targetIde
      plugin = prepareUsage(pluginSpec, "JsonPluginUsage", Dumps.JsonPluginUsage())
      kotlin = false
    }.run {
      with(compatibilityProblems) {
        assertEquals(1, size)
        assertContains(this, UndeclaredPluginDependencyProblem::class)
      }
    }
  }

  private fun Ide.assertHasBundledPluginWithPath(path: Path) {
    val hasPlugin = bundledPlugins.any {
      it.originalFile?.endsWith(path) ?: false
    }
    if (!hasPlugin) throw AssertionError("IDE does not contain plugin that has a path ending with '$path'")
  }

  fun findAfterIdeaBuildClassPath(): Path {
    val directory = Paths.get("after-idea", "build", "classes", "java", "main")
    return if (directory.exists()) {
      directory
    } else {
      Paths.get("verifier-test").resolve(directory).also { check(it.exists()) }
    }
  }

  private fun JsonParserDefinition(): ByteArray {
    return findAfterIdeaBuildClassPath()
      .resolve("com/intellij/json/JsonParserDefinition.class")
      .let { Files.readAllBytes(it) }
  }

  private val onlyJsonPluginProductInfoValue = """
    {
      "name": "IntelliJ IDEA",
      "version": "2024.3",
      "buildNumber": "243.16128",
      "productCode": "IC",
      "dataDirectoryName": "IntelliJIdea2024.3",
      "svgIconPath": "bin/idea.svg",
      "productVendor": "JetBrains",
      "bundledPlugins": [],
      "modules": [],
      "layout": [
        {
          "name": "com.intellij.modules.json",
          "kind": "plugin",
          "classPath": [
            "plugins/json/lib/json.jar"
          ]
        }            
      ]
    } 
  """.trimIndent()
}