package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.intellij.plugin.PluginV1Dependency
import com.jetbrains.plugin.structure.rules.FileSystemType
import com.jetbrains.plugin.structure.xinclude.withConditionalXIncludes
import com.jetbrains.plugin.structure.xinclude.withSystemProperty
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class XIncludePluginTest(fileSystemType: FileSystemType) : IdePluginManagerTest(fileSystemType) {

  @Test
  fun `xinclude includeUnless with system property being set to false`() {
    withConditionalXIncludes {
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
              file("applicationServices.xml", optionalDependencyXml)
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
  }

  @Test
  fun `xinclude includeUnless with system property being set to true`() {
    withConditionalXIncludes {
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
  }

  @Test
  fun `xinclude includeUnless with system property being set`() {
    withConditionalXIncludes {
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
        assertEquals(PluginV1Dependency.Optional("Optional Dependency"), optionalDescriptor.dependency)

        assert(plugin.extensions.isEmpty())
      }
    }
  }

  @Test
  fun `xinclude includeIf with system property`() {
    withConditionalXIncludes {
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
        assertEquals(PluginV1Dependency.Optional("Optional Dependency"), optionalDescriptor.dependency)

        assert(plugin.extensions.isEmpty())
      }
    }
  }

  @Test
  fun `xinclude includeIf conditional inclusion disabled`() {
    withConditionalXIncludes {
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
        assertEquals(PluginV1Dependency.Optional("Optional Dependency"), optionalDescriptor.dependency)

        assert(plugin.extensions.isEmpty())
      }
    }
  }

  @Test
  fun `conditional inclusion is disabled and conditional element with includeIf is completely ignored`() {
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
      assert(plugin.extensions.isEmpty())
    }
  }

  @Test
  fun `conditional inclusion is disabled and conditional element with includeUnless is completely ignored`() {
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
    assert(plugin.extensions.isEmpty())
  }

  private val optionalDependencyXml = """
    <idea-plugin>
      <extensions defaultExtensionNs="com.intellij">
        <applicationService serviceImplementation="com.jetbrains.plugins.structure.mocks.ProjectService"/>
      </extensions>
    </idea-plugin>
    """.trimIndent()

  @After
  fun tearDown() {
    close()
  }
}