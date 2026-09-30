/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.ModuleV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginV2Dependency
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DependencyConsistencyTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var plugin: IdePlugin

  @Before
  fun setUp() {
    plugin = buildPlugin(
      """
        <idea-plugin>
          <id>com.example.plugin</id>
          <name>Example plugin</name>
          <version>1.0</version>
          <vendor>JetBrains</vendor>
          <description>Example plugin used to verify content module dependency aggregation.</description>
          <change-notes>Example plugin used to verify content module dependency aggregation.</change-notes>
          <idea-version since-build="241.0"/>
          <dependencies>
            <module name="direct.module"/>
          </dependencies>
          <content>
            <module name="com.example.plugin.first"/>
            <module name="com.example.plugin.second"/>
          </content>
        </idea-plugin>
      """.trimIndent(),
      listOf(
        ModuleDescriptorSource(
          archive = "first.jar",
          module = "com.example.plugin.first",
          xml = """
            <idea-plugin>
              <dependencies>
                <module name="first.module"/>
                <plugin id="duplicate.plugin"/>
              </dependencies>
            </idea-plugin>
          """.trimIndent(),
        ),
        ModuleDescriptorSource(
          archive = "second.jar",
          module = "com.example.plugin.second",
          xml = """
            <idea-plugin>
              <dependencies>
                <plugin id="duplicate.plugin"/>
                <plugin id="second.plugin"/>
              </dependencies>
            </idea-plugin>
          """.trimIndent(),
        ),
      ),
    )
  }

  @Test
  fun `dependencies include direct and resolved content module dependencies`() {
    assertEquals(
      listOf("com.example.plugin.first", "com.example.plugin.second"),
      plugin.modulesDescriptors.map { it.name },
    )
    assertEquals(
      mapOf(
        (ModuleV2Dependency::class to "direct.module") to 1,
        (ModuleV2Dependency::class to "first.module") to 1,
        (PluginV2Dependency::class to "duplicate.plugin") to 2,
        (PluginV2Dependency::class to "second.plugin") to 1,
      ),
      plugin.dependencies.groupingBy { it::class to it.id }.eachCount(),
    )
    assertTrue(
      plugin.modulesDescriptors
        .flatMap { it.resolvedDependencies }
        .all { it.isOptional },
    )
  }

  @Test
  fun `reconstructDependencies contains direct dependencies only`() {
    assertEquals(
      listOf(ModuleV2Dependency("direct.module")),
      plugin.reconstructDependencies(),
    )
  }

  @Test
  fun `reconstructAllDependencies contains all dependencies and equals deprecated dependencies`() {
    val reconstructedDependencies = plugin.reconstructAllDependencies()

    assertEquals(plugin.dependencies, reconstructedDependencies)
    assertEquals(
      mapOf(
        (ModuleV2Dependency::class to "direct.module") to 1,
        (ModuleV2Dependency::class to "first.module") to 1,
        (PluginV2Dependency::class to "duplicate.plugin") to 2,
        (PluginV2Dependency::class to "second.plugin") to 1,
      ),
      reconstructedDependencies.groupingBy { it::class to it.id }.eachCount(),
    )
  }

  @Test
  fun `unresolved content modules do not contribute dependencies to the aggregate`() {
    val creationResult = buildPluginWithResult(
      """
        <idea-plugin>
          <id>com.example.plugin</id>
          <name>Example plugin</name>
          <version>1.0</version>
          <vendor>JetBrains</vendor>
          <description>Example plugin used to verify content module dependency aggregation.</description>
          <change-notes>Example plugin used to verify content module dependency aggregation.</change-notes>
          <idea-version since-build="241.0"/>
          <dependencies>
            <module name="direct.module"/>
          </dependencies>
          <content>
            <module name="com.example.plugin.resolved"/>
            <module name="com.example.plugin.ambiguous"/>
          </content>
        </idea-plugin>
      """.trimIndent(),
      listOf(
        ModuleDescriptorSource(
          archive = "resolved.jar",
          module = "com.example.plugin.resolved",
          xml = """
            <idea-plugin>
              <dependencies>
                <plugin id="resolved.plugin"/>
              </dependencies>
            </idea-plugin>
          """.trimIndent(),
        ),
        ModuleDescriptorSource(
          archive = "ambiguous-a.jar",
          module = "com.example.plugin.ambiguous",
          xml = """
            <idea-plugin>
              <dependencies>
                <module name="ambiguous.module"/>
              </dependencies>
            </idea-plugin>
          """.trimIndent(),
        ),
        ModuleDescriptorSource(
          archive = "ambiguous-b.jar",
          module = "com.example.plugin.ambiguous",
          xml = """
            <idea-plugin>
              <dependencies>
                <module name="ambiguous.module"/>
              </dependencies>
            </idea-plugin>
          """.trimIndent(),
        ),
      ),
    )

    assertTrue("Expected a successfully created plugin but got $creationResult", creationResult is PluginCreationSuccess)
    creationResult as PluginCreationSuccess

    assertEquals(listOf("com.example.plugin.resolved"), creationResult.plugin.modulesDescriptors.map { it.name })
    val ambiguityWarnings = creationResult.warnings.filter { it.message.contains("Found multiple plugin descriptors") }
    assertEquals("Expected a single ambiguity warning but got ${creationResult.warnings}", 1, ambiguityWarnings.size)
    assertTrue(ambiguityWarnings.single().message.contains("com.example.plugin.ambiguous"))
    assertEquals(
      mapOf(
        (ModuleV2Dependency::class to "direct.module") to 1,
        (PluginV2Dependency::class to "resolved.plugin") to 1,
      ),
      creationResult.plugin.dependencies.groupingBy { it::class to it.id }.eachCount(),
    )
  }

  private data class ModuleDescriptorSource(
    val archive: String,
    val module: String,
    val xml: String,
  )

  private fun buildPluginWithResult(
    pluginDescriptor: String,
    moduleDescriptorSources: List<ModuleDescriptorSource> = emptyList(),
  ): PluginCreationResult<IdePlugin> {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip").toPath()) {
      dir("plugin") {
        dir("lib") {
          zip("plugin.jar") {
            dir("META-INF") {
              file("plugin.xml", pluginDescriptor)
            }
          }
          moduleDescriptorSources
            .groupBy(ModuleDescriptorSource::archive)
            .forEach { (archiveName, descriptors) ->
              zip(archiveName) {
                descriptors.forEach { descriptor ->
                  file("${descriptor.module}.xml", descriptor.xml)
                }
              }
            }
        }
      }
    }
    return IdePluginManager.createManager().createPlugin(pluginFile, validateDescriptor = true)
  }

  private fun buildPlugin(
    pluginDescriptor: String,
    moduleDescriptorSources: List<ModuleDescriptorSource> = emptyList(),
  ): IdePlugin {
    val result = buildPluginWithResult(pluginDescriptor, moduleDescriptorSources)
    if (result is PluginCreationSuccess) {
      return result.plugin
    }
    fail("Expected successful plugin creation, but got $result")
    throw AssertionError("Expected successful plugin creation")
  }
}