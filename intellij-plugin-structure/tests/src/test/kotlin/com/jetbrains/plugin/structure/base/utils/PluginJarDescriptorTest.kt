package com.jetbrains.plugin.structure.base.utils

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.intellij.plugin.descriptors.IdeaPluginXmlDetector
import com.jetbrains.plugin.structure.jar.PluginJar
import com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class PluginJarDescriptorTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var pluginJarPath: Path

  private val ideaPluginXmlDetector = IdeaPluginXmlDetector()

  private val jarFileSystemProvider = SingletonCachingJarFileSystemProvider

  @Before
  fun setUp() {
    pluginJarPath = buildZipFile(temporaryFolder.newFile("plugin.jar").toPath()) {
      dir("META-INF") {
        file("plugin.xml") {
          """
          <idea-plugin>
            <name>JSON</name>
            <id>com.intellij.modules.json</id>
            <version>251.21418.62</version>
          </idea-plugin>            
          """.trimIndent()
        }
      }
      file("intellij.json.xml") {
        """
        <idea-plugin>
          <dependencies>
            <module name="intellij.json.split"/>
          </dependencies>
        </idea-plugin>                    
        """.trimIndent()
      }
    }
  }

  @Test
  fun `descriptors in META-INF and in roots are resolved`() {
    val descriptors = PluginJar(pluginJarPath, jarFileSystemProvider).resolveDescriptors()
    assertEquals(2, descriptors.size)
    assertTrue(descriptors.any { it.hasFileName("plugin.xml") })
    assertTrue(descriptors.any { it.hasFileName("intellij.json.xml") })
  }

  @Test
  fun `descriptors in META-INF and in roots are resolved with 'idea-plugin' detectiom`() {
    val descriptors = PluginJar(pluginJarPath, jarFileSystemProvider).resolveDescriptors(ideaPluginXmlDetector::isPluginDescriptor)
    assertEquals(2, descriptors.size)
    assertTrue(descriptors.any { it.hasFileName("plugin.xml") })
    assertTrue(descriptors.any { it.hasFileName("intellij.json.xml") })
  }

  private fun Path.hasFileName(fileName: String): Boolean {
    return fileName.toString() == fileName
  }
}