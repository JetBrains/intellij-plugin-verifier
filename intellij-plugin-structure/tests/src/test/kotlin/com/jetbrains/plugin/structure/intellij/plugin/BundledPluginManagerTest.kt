package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.ide.plugin.PluginIdExtractor
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.InputStream
import java.nio.file.Path

class BundledPluginManagerTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Before
  fun setUp() {
    val javaLibDir = temporaryFolder.newFolder("plugins", "java", "lib")
    val javaImplJar = javaLibDir.resolve("java-impl.jar").toPath()
    buildZipFile(javaImplJar) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              <id>com.intellij.java</id>
            </idea-plugin>
            """.trimIndent()
        }
      }
    }

    val javaCoverageDir = temporaryFolder.newFolder("plugins", "java-coverage", "lib")
    val javaCoverageJar = javaCoverageDir.resolve("java-coverage.jar").toPath()
    buildZipFile(javaCoverageJar) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              <name>Code Coverage for Java</name>
              <id>Coverage</id>
            </idea-plugin>
          """.trimIndent()
        }
      }
    }
  }

  @Test
  fun `plugin identifiers are retrieved`() {
    val pluginIdExtractor = PluginIdExtractor()
    val pluginIdProvider = object : PluginIdProvider {
      override fun getPluginId(pluginDescriptorStream: InputStream): String {
        return pluginIdExtractor.extractId(pluginDescriptorStream)
      }
    }
    val pluginManager = BundledPluginManager(pluginIdProvider)
    val pluginIdentifiers = pluginManager.getBundledPluginIds(idePath)
    assertTrue(pluginIdentifiers.contains("com.intellij.java"))
    assertTrue(pluginIdentifiers.contains("Coverage"))
  }

  private val idePath: Path
    get() = temporaryFolder.root.toPath()
}