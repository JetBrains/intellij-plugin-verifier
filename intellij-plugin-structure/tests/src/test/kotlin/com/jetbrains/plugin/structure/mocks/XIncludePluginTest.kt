package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Path

class XIncludePluginTest(fileSystemType: FileSystemType) : BasePluginManagerTest<IdePlugin, IdePluginManager>(fileSystemType) {

  @Test
  fun `xinclude includeUnless with system property being set to false`() {
    withSystemProperty("com.jetbrains.plugins.structure.mocks.optionalDependency", false) {
      val plugin = buildPluginSuccess(emptyList()) {
        buildZipFile(temporaryFolder.newFile("plugin.jar")) {
          dir("META-INF") {
            file("plugin.xml") {
              perfectXmlBuilder.modify {
                additionalContent = """
                <xi:include 
                  xmlns:xi="http://www.w3.org/2001/XInclude" 
                  href="applicationServices.xml" 
                  includeUnless="com.jetbrains.plugins.structure.mocks.optionalDependency"/>
              """.trimIndent()
              }
            }
            file("applicationServices.xml",optionalDependencyXml)
          }
        }
      }
      with(plugin.appContainerDescriptor.services) {
        assertEquals(1, size)
        val applicationService = first()
        assertEquals("com.jetbrains.plugins.structure.mocks.ProjectService", applicationService.serviceImplementation)
      }
    }
  }

  @Test
  fun `xinclude includeUnless with system property being set to true`() {
    withSystemProperty("com.jetbrains.plugins.structure.mocks.optionalDependency", true) {
      val plugin = buildPluginSuccess(emptyList()) {
        buildZipFile(temporaryFolder.newFile("plugin.jar")) {
          dir("META-INF") {
            file("plugin.xml") {
              perfectXmlBuilder.modify {
                additionalContent = """
                <xi:include 
                  xmlns:xi="http://www.w3.org/2001/XInclude" 
                  href="applicationServices.xml" 
                  includeUnless="com.jetbrains.plugins.structure.mocks.optionalDependency"/>
              """.trimIndent()
              }
            }
            file(
              "applicationServices.xml", optionalDependencyXml
            )
          }
        }
      }
      assertEquals(0, plugin.appContainerDescriptor.services.size)
    }
  }

  @Test
  fun `xinclude includeUnless with system property being set`() {
    withSystemProperty("com.jetbrains.plugins.structure.mocks.optionalDependency", true) {
      val plugin = buildPluginSuccess(emptyList()) {
        buildZipFile(temporaryFolder.newFile("plugin.jar")) {
          dir("META-INF") {
            file("plugin.xml") {
              perfectXmlBuilder.modify {
                depends += """<depends optional="true" config-file="optionalDependency.xml">Optional Dependency</depends>"""
                additionalContent = """
                <xi:include 
                  xmlns:xi="http://www.w3.org/2001/XInclude" 
                  href="optionalDependency.xml" 
                  includeUnless="com.jetbrains.plugins.structure.mocks.optionalDependency"/>
              """.trimIndent()
              }
            }
            file("optionalDependency.xml", optionalDependencyXml)
          }
        }
      }
      val optionalDescriptor = plugin.optionalDescriptors.single()
      assertEquals("optionalDependency.xml", optionalDescriptor.configurationFilePath)
      assertEquals(PluginDependencyImpl("Optional Dependency", true, false), optionalDescriptor.dependency)

      assert(plugin.extensions.isEmpty())
    }
  }

  @Test
  fun `xinclude includeIf with system property`() {
    withSystemProperty("com.jetbrains.plugins.structure.mocks.optionalDependency", true) {
      val plugin = buildPluginSuccess(emptyList()) {
        buildZipFile(temporaryFolder.newFile("plugin.jar")) {
          dir("META-INF") {
            file("plugin.xml") {
              perfectXmlBuilder.modify {
                depends += """<depends optional="true" config-file="optionalDependency.xml">Optional Dependency</depends>"""
                additionalContent = """
                <xi:include 
                  xmlns:xi="http://www.w3.org/2001/XInclude" 
                  href="optionalDependency.xml" 
                  includeIf="com.jetbrains.plugins.structure.mocks.optionalDependency"/>
              """.trimIndent()
              }
            }
            file("optionalDependency.xml", optionalDependencyXml)
          }
        }
      }
      val optionalDescriptor = plugin.optionalDescriptors.single()
      assertEquals("optionalDependency.xml", optionalDescriptor.configurationFilePath)
      assertEquals(PluginDependencyImpl("Optional Dependency", true, false), optionalDescriptor.dependency)

      assert(plugin.extensions.isEmpty())
    }
  }

  override fun createManager(extractDirectory: Path): IdePluginManager =
    IdePluginManager.createManager(extractDirectory)

  private fun buildPluginSuccess(expectedWarnings: List<PluginProblem>, pluginFactory: IdePluginFactory = ::defaultPluginFactory, pluginFileBuilder: () -> Path): IdePlugin {
    val pluginFile = pluginFileBuilder()
    val successResult = createPluginSuccessfully(pluginFile, pluginFactory)
    val (plugin, warnings) = successResult
    assertEquals(expectedWarnings.toSet().sortedBy { it.message }, warnings.toSet().sortedBy { it.message })
    assertEquals(pluginFile, plugin.originalFile)
    return plugin
  }

  private fun withSystemProperty(property: String, value: Boolean, block: () -> Unit) {
    try {
      System.setProperty(property, value.toString())
      block()
    } finally {
      System.clearProperty(property)
    }
  }

  private val optionalDependencyXml = """
    <idea-plugin>
      <extensions defaultExtensionNs="com.intellij">
        <applicationService serviceImplementation="com.jetbrains.plugins.structure.mocks.ProjectService"/>
      </extensions>
    </idea-plugin>
    """.trimIndent()


}