/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.analysis

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.FixedClassesResolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.pluginverifier.analysis.ReachabilityGraph.ReachabilityMark.OPTIONAL_PLUGIN
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.dependencies.MissingDependency
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

  private fun buildPlugin(declareOptionalDependencyInPluginXml: Boolean): IdePlugin {
    val optionalDepends = if (declareOptionalDependencyInPluginXml) {
      """<depends optional="true" config-file="optional-x.xml">$OPTIONAL_PLUGIN_ID</depends>"""
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
private const val HELPER_CLASS = "com/example/module/Helper"
