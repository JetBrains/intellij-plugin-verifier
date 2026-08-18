package com.jetbrains.plugin.structure.intellij.plugin.module

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider
import com.jetbrains.plugin.structure.zipBombs.getZipWithoutEocd
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.io.path.invariantSeparatorsPathString

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
    val resolvedClassPath = contentModules.resolvedClassPath.map { root.relativize(it).invariantSeparatorsPathString }.sorted()

    assertEquals(expectedClassPath, resolvedClassPath)
  }

  @Test
  fun `descriptor index maps a descriptor file name to every JAR that provides it`() {
    val root = temporaryFolder.root.toPath()

    val pluginPath = buildDirectory(temporaryFolder.newFolder("scala").toPath()) {
      dir("lib") {
        zip("scala.jar") {
          dir("META-INF") {
            file("plugin.xml", "<idea-plugin />")
          }
          file("intellij.scala.xml", "<idea-plugin />")
        }
        zip("scala-shadowed.jar") {
          file("intellij.scala.xml", "<idea-plugin />")
        }
        zip("scala-auxiliary.jar") {
          file("logback.xml", "<configuration />")
        }
      }
    }

    val descriptorIndex = ContentModuleScanner(jarFileSystemProvider).getDescriptorIndex(pluginPath)

    val indexedJarPaths = descriptorIndex.mapValues { (_, jarPaths) ->
      jarPaths.map { root.relativize(it).invariantSeparatorsPathString }.sorted()
    }
    assertEquals(
      mapOf(
        "plugin.xml" to listOf("scala/lib/scala.jar"),
        "intellij.scala.xml" to listOf("scala/lib/scala-shadowed.jar", "scala/lib/scala.jar")
      ),
      indexedJarPaths
    )
  }

  @Test
  fun `no content modules are found in a corrupted artifact`() {
    val pluginPath = buildDirectory(temporaryFolder.newFolder("corrupted").toPath()) {
      dir("lib") {
        getZipWithoutEocd()?.let {
          file("corrupted.jar", it)
        }
      }
    }

    val contentModuleScanner = ContentModuleScanner(jarFileSystemProvider)
    val contentModules = contentModuleScanner.getContentModules(pluginPath)
    assertTrue(contentModules.modules.isEmpty())
  }
}