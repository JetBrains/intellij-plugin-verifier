/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.intellij.resources.DefaultResourceResolver
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.FileSystem
import java.nio.file.Path

/**
 * Verifies how a content module descriptor that resides in a JAR of the plugin `lib` directory is resolved.
 *
 * Such a descriptor is searched for in the whole `lib` directory, as its declaration in `plugin.xml` does not
 * indicate which JAR provides it.
 */
class ContentModuleDescriptorResolutionTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `content module descriptor available in a single JAR is resolved with the plugin classpath`() {
    val pluginPath = buildPlugin("single-provider", moduleNames = listOf("example.module"), moduleJarNames = listOf("modules.jar"), auxiliaryJarCount = 3)

    val plugin = createPlugin(pluginPath)

    val moduleDescriptor = plugin.modulesDescriptors.single()
    assertEquals("example.module", moduleDescriptor.name)
    /*
    The module is loaded via the plugin `lib` directory, hence it is aware of the whole plugin classpath.
    Resolving it directly from its own JAR would leave the classpath empty.
    */
    assertTrue(moduleDescriptor.module.classpath.entries.isNotEmpty())
  }

  @Test
  fun `content module descriptor available in multiple JARs is reported as ambiguous and is not resolved`() {
    val pluginPath = buildPlugin(
      "multiple-providers",
      moduleNames = listOf("example.module"),
      moduleJarNames = listOf("modules-a.jar", "modules-b.jar"),
      auxiliaryJarCount = 3
    )

    val creationResult = createManager(SingletonCachingJarFileSystemProvider).createPlugin(pluginPath, false)
    assertTrue("Expected a successfully created plugin but got $creationResult", creationResult is PluginCreationSuccess)
    creationResult as PluginCreationSuccess

    assertTrue("Expected an unresolved module but got ${creationResult.plugin.modulesDescriptors}", creationResult.plugin.modulesDescriptors.isEmpty())

    val ambiguityWarnings = creationResult.warnings.filter { it.message.contains("Found multiple plugin descriptors") }
    assertEquals("Expected a single ambiguity warning but got ${creationResult.warnings}", 1, ambiguityWarnings.size)
    assertTrue(ambiguityWarnings.single().message.contains("example.module"))
  }

  @Test
  fun `content module descriptor available in a ZIP of the lib directory is resolved`() {
    /*
    A `lib` directory archive is searched for a descriptor whether it is a JAR or a ZIP, so the descriptor
    index must cover both. Indexing JARs alone silently left such a content module unresolved.
    Note that a ZIP is still absent from the resolved classpath, which is derived from JARs only.
    */
    val pluginPath = buildPlugin("zip-provider", moduleNames = listOf("example.module"), moduleJarNames = listOf("modules.zip"), auxiliaryJarCount = 3)

    val plugin = createPlugin(pluginPath)

    assertEquals("example.module", plugin.modulesDescriptors.single().name)
  }

  @Test
  fun `content module descriptor available in both a JAR and a ZIP is reported as ambiguous and is not resolved`() {
    val pluginPath = buildPlugin(
      "mixed-archive-providers",
      moduleNames = listOf("example.module"),
      moduleJarNames = listOf("modules-a.jar", "modules-b.zip"),
      auxiliaryJarCount = 3
    )

    val creationResult = createManager(SingletonCachingJarFileSystemProvider).createPlugin(pluginPath, false)
    assertTrue("Expected a successfully created plugin but got $creationResult", creationResult is PluginCreationSuccess)
    creationResult as PluginCreationSuccess

    assertTrue("Expected an unresolved module but got ${creationResult.plugin.modulesDescriptors}", creationResult.plugin.modulesDescriptors.isEmpty())

    val ambiguityWarnings = creationResult.warnings.filter { it.message.contains("Found multiple plugin descriptors") }
    assertEquals("Expected a single ambiguity warning but got ${creationResult.warnings}", 1, ambiguityWarnings.size)
    assertTrue(ambiguityWarnings.single().message.contains("example.module"))
  }

  @Test
  fun `content module descriptors are not searched for in every JAR of the plugin`() {
    val moduleNames = (1..10).map { "example.module$it" }

    val fewJars = countJarFileSystems("few-auxiliary-jars", moduleNames, auxiliaryJarCount = 5)
    val manyJars = countJarFileSystems("many-auxiliary-jars", moduleNames, auxiliaryJarCount = 45)

    /*
    A JAR that provides no descriptor of this plugin is opened a constant number of times per plugin:
    to resolve the plugin descriptor, to index the `lib` directory and to resolve the plugin classpath.
    Adding such JARs must not cost anything per content module, as it did when every content module was
    searched for in every JAR of the `lib` directory.
    */
    val addedJars = 45 - 5
    val growthPerAddedJar = (manyJars - fewJars).toDouble() / addedJars
    assertTrue(
      "Opening $addedJars more JARs that provide no descriptor grew the number of opened JAR file systems " +
        "from $fewJars to $manyJars, which is ${"%.1f".format(growthPerAddedJar)} per added JAR. " +
        "It must not scale with the ${moduleNames.size} content modules of the plugin.",
      growthPerAddedJar < 6
    )
  }

  private fun countJarFileSystems(pluginName: String, moduleNames: List<String>, auxiliaryJarCount: Int): Int {
    val pluginPath = buildPlugin(pluginName, moduleNames, moduleJarNames = listOf("modules.jar"), auxiliaryJarCount = auxiliaryJarCount)
    val countingProvider = CountingJarFileSystemProvider(SingletonCachingJarFileSystemProvider)

    val plugin = createPlugin(pluginPath, countingProvider)
    assertEquals(moduleNames.size, plugin.modulesDescriptors.size)

    return countingProvider.getFileSystemCalls
  }

  private fun createPlugin(pluginPath: Path, fileSystemProvider: JarFileSystemProvider = SingletonCachingJarFileSystemProvider): IdePlugin {
    val creationResult = createManager(fileSystemProvider).createPlugin(pluginPath, false)
    assertTrue("Expected a successfully created plugin but got $creationResult", creationResult is PluginCreationSuccess)
    return (creationResult as PluginCreationSuccess).plugin
  }

  private fun createManager(fileSystemProvider: JarFileSystemProvider) =
    IdePluginManager.createManager(DefaultResourceResolver, temporaryFolder.newFolder().toPath(), fileSystemProvider)

  /**
   * Builds a plugin directory where each of [moduleJarNames] provides a descriptor of every module of
   * [moduleNames], accompanied by [auxiliaryJarCount] JARs that provide no descriptor at all.
   */
  private fun buildPlugin(
    pluginName: String,
    moduleNames: List<String>,
    moduleJarNames: List<String>,
    auxiliaryJarCount: Int
  ): Path = buildDirectory(temporaryFolder.newFolder(pluginName).toPath()) {
    dir("lib") {
      zip("main.jar") {
        dir("META-INF") {
          file("plugin.xml", pluginXml(moduleNames))
        }
      }
      moduleJarNames.forEach { moduleJarName ->
        zip(moduleJarName) {
          moduleNames.forEach { moduleName ->
            file("$moduleName.xml", "<idea-plugin />")
          }
        }
      }
      repeat(auxiliaryJarCount) { index ->
        zip("auxiliary$index.jar") {
          file("auxiliary.txt", "Not a plugin descriptor")
        }
      }
    }
  }

  private fun pluginXml(moduleNames: List<String>): String {
    val modules = moduleNames.joinToString(separator = "\n      ") { """<module name="$it"/>""" }
    return """
      <idea-plugin>
        <id>com.example.plugin</id>
        <name>Example</name>
        <version>1.0</version>
        <vendor>JetBrains</vendor>
        <description>A plugin with content modules declared in JARs of the lib directory.</description>
        <idea-version since-build="241.0"/>
        <dependencies>
          <plugin id="com.intellij.modules.platform"/>
        </dependencies>
        <content>
          $modules
        </content>
      </idea-plugin>
    """.trimIndent()
  }

  private class CountingJarFileSystemProvider(private val delegate: JarFileSystemProvider) : JarFileSystemProvider {
    var getFileSystemCalls = 0
      private set

    override fun getFileSystem(jarPath: Path): FileSystem {
      getFileSystemCalls += 1
      return delegate.getFileSystem(jarPath)
    }

    override fun getFileSystem(jarPath: Path, configuration: JarFileSystemProvider.Configuration): FileSystem {
      getFileSystemCalls += 1
      return delegate.getFileSystem(jarPath, configuration)
    }
  }
}
