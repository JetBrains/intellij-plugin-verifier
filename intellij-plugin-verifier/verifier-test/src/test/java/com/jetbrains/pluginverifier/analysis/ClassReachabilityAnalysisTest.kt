/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.analysis

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.classes.resolvers.DelegatingNamedResolver
import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.FixedClassesResolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.pluginverifier.analysis.ReachabilityGraph.ReachabilityMark.MAIN_PLUGIN
import com.jetbrains.pluginverifier.analysis.ReachabilityGraph.ReachabilityMark.OPTIONAL_PLUGIN
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode

class ClassReachabilityAnalysisTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `content module classes are optional when the module depends on a missing optional plugin`() {
    val plugin = buildPlugin(declareOptionalDependencyInPluginXml = false)

    assertModuleClassesReachableFromOptionalMark(plugin)
  }

  @Test
  fun `content module classes are optional when plugin xml declares the same missing optional plugin`() {
    val plugin = buildPlugin(declareOptionalDependencyInPluginXml = true)

    assertModuleClassesReachableFromOptionalMark(plugin)
  }

  @Test
  fun `shared type membership is checked once per graph build without changing reachability`() {
    val plugin = buildPlugin(declareOptionalDependencyInPluginXml = false, includeMainService = true)
    val pluginNode = DependencyNode.dependencyNode(plugin)
    val dependenciesGraph = DependenciesGraph(
      verifiedPlugin = pluginNode,
      vertices = setOf(pluginNode),
      edges = emptySet(),
      missingDependencies = mapOf(
        pluginNode to setOf(MissingDependency(PluginDependencyImpl(OPTIONAL_PLUGIN_ID, true, false), "not found"))
      )
    )
    val classes = listOf(MODULE_SERVICE_CLASS, MAIN_SERVICE_CLASS).map { serviceClass ->
      classNode(serviceClass).apply {
        fields.add(FieldNode(Opcodes.ACC_PRIVATE, "helper", "L$HELPER_CLASS;", null, null))
        fields.add(FieldNode(Opcodes.ACC_PRIVATE, "external", "L$EXTERNAL_CLASS;", null, null))
      }
    } + classNode(HELPER_CLASS)
    val delegate = FixedClassesResolver.create(classes, object : FileOrigin { override val parent = null })
    val membershipCalls = hashMapOf<String, Int>()
    val countingResolver = object : DelegatingNamedResolver("counting", { delegate }) {
      override fun containsClass(className: String): Boolean {
        membershipCalls[className] = (membershipCalls[className] ?: 0) + 1
        return delegate.containsClass(className)
      }
    }

    repeat(2) { buildIndex ->
      val graph = buildClassReachabilityGraph(plugin, countingResolver, dependenciesGraph)

      assertTrue(graph.isClassReachableFromMark(HELPER_CLASS, OPTIONAL_PLUGIN))
      assertTrue(graph.isClassReachableFromMark(HELPER_CLASS, MAIN_PLUGIN))
      assertFalse(graph.isClassReachableFromMark(EXTERNAL_CLASS, OPTIONAL_PLUGIN))
      assertFalse(graph.isClassReachableFromMark(EXTERNAL_CLASS, MAIN_PLUGIN))
      assertEquals(buildIndex + 1, membershipCalls[HELPER_CLASS])
      assertEquals(buildIndex + 1, membershipCalls[EXTERNAL_CLASS])
    }
  }

  private fun assertModuleClassesReachableFromOptionalMark(plugin: IdePlugin) {
    val pluginNode = DependencyNode.dependencyNode(plugin)
    val dependenciesGraph = DependenciesGraph(
      verifiedPlugin = pluginNode,
      vertices = setOf(pluginNode),
      edges = emptySet(),
      missingDependencies = mapOf(
        pluginNode to setOf(MissingDependency(PluginDependencyImpl(OPTIONAL_PLUGIN_ID, true, false), "not found"))
      )
    )

    val reachabilityGraph = buildClassReachabilityGraph(plugin, resolver, dependenciesGraph)

    assertTrue(
      "Class '$HELPER_CLASS' referenced from content module class '$MODULE_SERVICE_CLASS' must be reachable from an optional plugin",
      reachabilityGraph.isClassReachableFromMark(HELPER_CLASS, OPTIONAL_PLUGIN)
    )
  }

  private fun buildPlugin(declareOptionalDependencyInPluginXml: Boolean, includeMainService: Boolean = false): IdePlugin {
    val optionalDepends = if (declareOptionalDependencyInPluginXml) {
      """<depends optional="true" config-file="optional-x.xml">$OPTIONAL_PLUGIN_ID</depends>"""
    } else {
      ""
    }
    val mainService = if (includeMainService) {
      """
        <extensions defaultExtensionNs="com.intellij">
          <applicationService serviceImplementation="${MAIN_SERVICE_CLASS.replace('/', '.')}"/>
        </extensions>
      """.trimIndent()
    } else {
      ""
    }
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip").toPath()) {
      dir("plugin") {
        dir("lib") {
          zip("plugin.jar") {
            dir("META-INF") {
              file(
                "plugin.xml",
                """
                  <idea-plugin>
                    <id>com.example.plugin</id>
                    <name>Example plugin</name>
                    <version>1.0</version>
                    <vendor>JetBrains</vendor>
                    <description>Example plugin used to verify class reachability analysis.</description>
                    <change-notes>Example plugin used to verify class reachability analysis.</change-notes>
                    <idea-version since-build="241.0"/>
                    $optionalDepends
                    $mainService
                    <content>
                      <module name="$MODULE_NAME"/>
                    </content>
                  </idea-plugin>
                """.trimIndent()
              )
              file("optional-x.xml", "<idea-plugin/>")
            }
          }
          zip("module.jar") {
            file(
              "$MODULE_NAME.xml",
              """
                <idea-plugin>
                  <dependencies>
                    <plugin id="$OPTIONAL_PLUGIN_ID"/>
                  </dependencies>
                  <extensions defaultExtensionNs="com.intellij">
                    <applicationService serviceImplementation="${MODULE_SERVICE_CLASS.replace('/', '.')}"/>
                  </extensions>
                </idea-plugin>
              """.trimIndent()
            )
          }
        }
      }
    }
    val creationResult = IdePluginManager.createManager().createPlugin(pluginFile, validateDescriptor = true)
    if (creationResult !is PluginCreationSuccess) {
      fail("Expected successful plugin creation, but got $creationResult")
    }
    return (creationResult as PluginCreationSuccess).plugin
  }

  private val resolver = FixedClassesResolver.create(
    listOf(
      classNode(MODULE_SERVICE_CLASS).apply {
        fields.add(FieldNode(Opcodes.ACC_PRIVATE, "helper", "L$HELPER_CLASS;", null, null))
      },
      classNode(HELPER_CLASS)
    ),
    object : FileOrigin {
      override val parent = null
    }
  )

  private fun classNode(name: String) = ClassNode().apply {
    version = Opcodes.V11
    access = Opcodes.ACC_PUBLIC
    this.name = name
    superName = "java/lang/Object"
  }
}

private const val OPTIONAL_PLUGIN_ID = "com.example.x"
private const val MODULE_NAME = "com.example.plugin.module"
private const val MODULE_SERVICE_CLASS = "com/example/module/ModuleService"
private const val MAIN_SERVICE_CLASS = "com/example/MainService"
private const val HELPER_CLASS = "com/example/module/Helper"
private const val EXTERNAL_CLASS = "external/SharedType"
