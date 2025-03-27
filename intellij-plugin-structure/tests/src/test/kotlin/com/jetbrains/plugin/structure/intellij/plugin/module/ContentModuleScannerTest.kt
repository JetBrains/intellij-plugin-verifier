package com.jetbrains.plugin.structure.intellij.plugin.module

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ContentModuleScannerTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private val jarFileSystemProvider = SingletonCachingJarFileSystemProvider

  @Test
  fun `root module and 2 additional modules are resolved`() {
    val root = temporaryFolder.root.toPath()

    val pluginPath = buildDirectory(temporaryFolder.newFolder("json").toPath()) {
      dir("lib") {
        dir("modules") {
          zip("intellij.json.split.jar") {
            file("intellij.json.split.xml", "<idea-plugin />")
          }
        }
        zip("json.jar") {
          dir("META-INF") {
            file("plugin.xml", "<idea-plugin />")
          }
          file("intellij.json.xml", "<idea-plugin />")
        }
      }
    }

    val contentModuleScanner = ContentModuleScanner(jarFileSystemProvider)
    val contentModules = contentModuleScanner.getContentModules(pluginPath)

    with(contentModules.modules) {
      assertEquals(3, size)
      val identifiers = map { it.id }
      assertTrue(identifiers.contains("intellij.json.split"))
      assertTrue(identifiers.contains("intellij.json"))
    }

    /*
    json.jar is intentionally put twice:
    - once for the plugin itself,
    - and another time as a module with descriptor in the root of JAR
    */
    val expectedClassPath = listOf("json/lib/json.jar", "json/lib/json.jar", "json/lib/modules/intellij.json.split.jar")
    val resolvedClassPath = contentModules.resolvedClassPath.map { root.relativize(it).toString() }.sorted()

    assertEquals(expectedClassPath, resolvedClassPath)
  }
}