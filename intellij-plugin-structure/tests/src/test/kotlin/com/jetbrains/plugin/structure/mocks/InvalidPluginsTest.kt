package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.problems.*
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder
import java.io.File

class InvalidPluginsTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Rule
  @JvmField
  val expectedEx: ExpectedException = ExpectedException.none()

  companion object {
    fun assertExpectedProblems(pluginFile: File, expectedProblems: List<PluginProblem>) {
      val creationFail = getFailedResult(pluginFile)
      val actualProblems = creationFail.errorsAndWarnings
      assertEquals(expectedProblems.toSet(), actualProblems.toSet())
    }

    fun getFailedResult(pluginFile: File): PluginCreationFail<IdePlugin> {
      val pluginCreationResult = IdePluginManager.createManager().createPlugin(pluginFile)
      if (pluginCreationResult is PluginCreationSuccess) {
        Assert.fail("must have failed, but warnings: [${pluginCreationResult.warnings.joinToString()}]")
      }
      return pluginCreationResult as PluginCreationFail
    }

    fun getSuccessResult(pluginFile: File): PluginCreationSuccess<IdePlugin> {
      val pluginCreationResult = IdePluginManager.createManager().createPlugin(pluginFile)
      if (pluginCreationResult is PluginCreationFail) {
        Assert.fail(pluginCreationResult.errorsAndWarnings.joinToString())
      }
      return pluginCreationResult as PluginCreationSuccess<IdePlugin>
    }
  }

  @Test
  fun `incorrect plugin file type`() {
    val incorrect = temporaryFolder.newFile("incorrect.txt")
    assertExpectedProblems(incorrect, listOf(createIncorrectIntellijFileProblem(incorrect.name)))
  }

  @Test
  fun `failed to read jar file`() {
    val incorrect = temporaryFolder.newFile("incorrect.jar")
    assertExpectedProblems(incorrect, listOf(UnableToReadJarFile()))
  }

  @Test
  fun `plugin file does not exist`() {
    val nonExistentFile = File("non-existent-file")
    expectedEx.expect(IllegalArgumentException::class.java)
    expectedEx.expectMessage("Plugin file non-existent-file does not exist")
    IdePluginManager.createManager().createPlugin(nonExistentFile)
  }

  @Test
  fun `unable to extract plugin`() {
    val brokenZipArchive = temporaryFolder.newFile("broken.zip")
    assertExpectedProblems(brokenZipArchive, listOf(PluginZipIsEmpty()))
  }

  @Test
  fun `no meta-inf plugin xml found`() {
    val folder = temporaryFolder.newFolder()
    assertExpectedProblems(folder, listOf(PluginDescriptorIsNotFound("plugin.xml")))
  }

  @Test
  fun `plugin description is empty`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        description = "<description></description>"
      },
      listOf(PropertyNotSpecified("description", "plugin.xml"))
    )
  }

  @Test
  fun `plugin name is not specified`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        name = ""
      },
      listOf(PropertyNotSpecified("name", "plugin.xml"))
    )
  }

  @Test
  fun `plugin name contains newline`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        name = "<name>Some\nname</name>"
      },
      listOf(ContainsNewlines("name", "plugin.xml"))
    )
  }

  @Test
  fun `plugin name contains newline at end`() {
    val pluginName = "Some name"
    val plugin = `test valid plugin xml`(
      perfectXmlBuilder.modify {
        name = "<name>$pluginName\n </name>"
      }
    )
    assertEquals(pluginName, plugin.plugin.pluginName)
  }

  @Test
  fun `plugin id contains newline`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        id = "<id>some\nId</id>"
      },
      listOf(ContainsNewlines("id", "plugin.xml"))
    )
  }

  @Test
  fun `plugin id contains newline at end`() {
    val pluginId = "someId"
    val plugin = `test valid plugin xml`(
      perfectXmlBuilder.modify {
        id = "<id>$pluginId\n </id>"
      }
    )
    assertEquals(pluginId, plugin.plugin.pluginId)
  }

  @Test
  fun `plugin id is not specified but it is equal to name`() {
    val pluginXmlContent = perfectXmlBuilder.modify {
      id = ""
    }
    val pluginFolder = getTempPluginFolder(pluginXmlContent)
    val successResult = getSuccessResult(pluginFolder)
    val plugin = successResult.plugin
    assertEquals(plugin.pluginName, plugin.pluginId)
  }

  @Test
  fun `plugin vendor is not specified`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        vendor = ""
      },
      listOf(PropertyNotSpecified("vendor", "plugin.xml"))
    )
  }

  @Test
  fun `plugin version is not specified`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        version = ""
      }
      , listOf(PropertyNotSpecified("version", "plugin.xml")))
  }

  @Test
  fun `idea version is not specified`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        ideaVersion = ""
      },
      listOf(PropertyNotSpecified("idea-version", "plugin.xml"))
    )
  }

  @Test
  fun `invalid empty dependency bean`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        depends = "<depends></depends>"
      },
      listOf(InvalidDependencyId("plugin.xml", ""))
    )
  }

  @Test
  fun `invalid dependency tag containing new line characters bean`() {
    val dependencyId = "\ncom.intellij.modules.java\n"
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        depends = "<depends>$dependencyId</depends>"
      },
      listOf(InvalidDependencyId("plugin.xml", dependencyId))
    )
  }

  @Test
  fun `superfluous dependency declaration`() {
    val dependencyId = "superfluousDependencyId"
    `test plugin xml warnings`(
      perfectXmlBuilder.modify {
        depends += "\n"
        depends += "<depends optional=\"false\">$dependencyId</depends>"
      },
      listOf(SuperfluousNonOptionalDependencyDeclaration(dependencyId))
    )
  }

  @Test
  fun `config-file must be specified`() {
    val dependencyId = "someDependencyId"
    `test plugin xml warnings`(
      perfectXmlBuilder.modify {
        depends += "\n"
        depends += "<depends optional=\"true\">$dependencyId</depends>"
      },
      listOf(OptionalDependencyConfigFileNotSpecified(dependencyId))
    )
  }

  @Test
  fun `invalid module bean`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        modules = listOf("")
      },
      listOf(InvalidModuleBean("plugin.xml"))
    )
  }

  @Test
  fun `missing since build`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        ideaVersion = "<idea-version/>"
      },
      listOf(SinceBuildNotSpecified("plugin.xml"))
    )
  }

  @Test
  fun `invalid since build`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        ideaVersion = """<idea-version since-build="131."/>"""
      },
      listOf(InvalidSinceBuild("plugin.xml", "131."))
    )
  }

  @Test
  fun `invalid until build`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        ideaVersion = """<idea-version since-build="131.1" until-build="141."/>"""
      },
      listOf(InvalidUntilBuild("plugin.xml", "141."))
    )
  }

  @Test
  fun `since build less then until `() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        ideaVersion = """<idea-version since-build="131.1" until-build="120.1"/>"""
      },
      listOf(SinceBuildGreaterThanUntilBuild("plugin.xml", IdeVersion.createIdeVersion("131.1"), IdeVersion.createIdeVersion("120.1")))
    )
  }

  @Test
  fun `empty vendor`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        vendor = """<vendor></vendor>"""
      },
      listOf(PropertyNotSpecified("vendor", "plugin.xml"))
    )

    `test valid plugin xml`(
      perfectXmlBuilder.modify {
        vendor = """<vendor>Vendor name</vendor>"""
      })

    `test valid plugin xml`(
      perfectXmlBuilder.modify {
        vendor = """<vendor url="https://vendor.url"></vendor>"""
      })

    `test valid plugin xml`(
      perfectXmlBuilder.modify {
        vendor = """<vendor email="vendor@email.com"></vendor>"""
      })
  }

  @Test
  fun `wildcard in old ide`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        ideaVersion = """<idea-version since-build="129.0.*"/>"""
      },
      listOf(InvalidSinceBuild("plugin.xml", "129.0.*"))
    )
  }

  @Test
  fun `non latin description`() {
    `test plugin xml warnings`(
      perfectXmlBuilder.modify {
        description = "<description>Описание без английского, но достаточно длинное</description>"
      },
      listOf(NonLatinDescription())
    )
  }

  @Test
  fun `html description`() {
    `test plugin xml warnings`(
      perfectXmlBuilder.modify {
        description = """<description><![CDATA[
          <a href=\"https://github.com/myamazinguserprofile/myamazingproject\">short text</a>
          ]]></description>"""
      },
      listOf(ShortDescription())
    )
  }

  @Test
  fun `default values`() {
    `test invalid plugin xml`(
      """<idea-plugin>
      <id>com.your.company.unique.plugin.id</id>
      <name>Plugin display name here</name>
      <version>1.0</version>
      <vendor email="support@yourcompany.com" url="https://www.yourcompany.com">YourCompany</vendor>
      <description><![CDATA[
        Enter short description for your plugin here.<br>
        <em>most HTML tags may be used</em>
      ]]></description>
      <change-notes><![CDATA[
          Add change notes here.<br>
          <em>most HTML tags may be used</em>
        ]]>
      </change-notes>
      <idea-version since-build="145.0"/>
      <extensions defaultExtensionNs="com.intellij">
      </extensions>
      <actions>
      </actions>
    </idea-plugin>
      """, listOf(
      PropertyWithDefaultValue("plugin.xml", PropertyWithDefaultValue.DefaultProperty.ID),
      PropertyWithDefaultValue("plugin.xml", PropertyWithDefaultValue.DefaultProperty.NAME),
      DefaultDescription("plugin.xml"),
      DefaultChangeNotes("plugin.xml"),
      PropertyWithDefaultValue("plugin.xml", PropertyWithDefaultValue.DefaultProperty.VENDOR),
      PropertyWithDefaultValue("plugin.xml", PropertyWithDefaultValue.DefaultProperty.VENDOR_URL),
      PropertyWithDefaultValue("plugin.xml", PropertyWithDefaultValue.DefaultProperty.VENDOR_EMAIL)
    )
    )
  }

  private fun `test plugin xml warnings`(pluginXmlContent: String, expectedWarnings: List<PluginProblem>) {
    val pluginFolder = getTempPluginFolder(pluginXmlContent)
    val successResult = getSuccessResult(pluginFolder)
    assertEquals(expectedWarnings, successResult.warnings)
  }

  private fun `test invalid plugin xml`(pluginXmlContent: String, expectedProblems: List<PluginProblem>) {
    val pluginFolder = getTempPluginFolder(pluginXmlContent)
    assertExpectedProblems(pluginFolder, expectedProblems)
  }

  private fun `test valid plugin xml`(pluginXmlContent: String): PluginCreationSuccess<IdePlugin> {
    val pluginFolder = getTempPluginFolder(pluginXmlContent)
    return getSuccessResult(pluginFolder)
  }

  private fun getTempPluginFolder(pluginXmlContent: String): File {
    return buildDirectory(temporaryFolder.newFolder()) {
      dir("META-INF") {
        file("plugin.xml", pluginXmlContent)
      }
    }
  }

  @Test
  fun `plugin has multiple plugin descriptors in lib directory where descriptor might miss mandatory elements`() {
    /*
    plugin/
      lib/
        one.jar!/
          META-INF/
            plugin.xml  <--- duplicated
        two.jar!/
          META-INF/
            plugin.xml  <--- duplicated
    */

    val pluginDirectory = buildDirectory(temporaryFolder.newFolder("plugin")) {
      dir("lib") {
        zip("one.jar") {
          dir("META-INF") {
            file("plugin.xml", perfectXmlBuilder.modify { })
          }
        }

        zip("two.jar") {
          dir("META-INF") {
            file("plugin.xml", perfectXmlBuilder.modify { })
          }
        }
      }
    }

    assertExpectedProblems(pluginDirectory, listOf(MultiplePluginDescriptors("plugin.xml", "one.jar", "plugin.xml", "two.jar")))
  }

  @Test
  fun `completely invalid plugin descriptor`() {
    `test invalid plugin xml`(
      "abracadabra",
      listOf(UnexpectedDescriptorElements("unexpected element on line 1", "plugin.xml"))
    )
  }

  @Test
  fun `plugin specifies unresolved xinclude element`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        ideaPluginTagOpen = """<idea-plugin xmlns:xi="http://www.w3.org/2001/XInclude">"""
        additionalContent = """<xi:include href="/META-INF/missing.xml" xpointer="xpointer(/idea-plugin/*)"/>"""
      }, listOf(XIncludeResolutionErrors("plugin.xml", "Not found document '/META-INF/missing.xml' referenced in <xi:include href=\"/META-INF/missing.xml\", xpointer=\"xpointer(/idea-plugin/*)\"/>. <xi:fallback> element is not provided. (at plugin.xml)"))
    )
  }

  @Test
  fun `plugin specifies unresolved theme path`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        additionalContent = """
            <extensions defaultExtensionNs="com.intellij">
    <themeProvider id="someId" path="/unresolved.theme.theme.json"/>
  </extensions>
          """.trimIndent()
      }, listOf(UnableToFindTheme("plugin.xml", "/unresolved.theme.theme.json"))
    )
  }

  @Test
  fun `since or until build with year instead of branch number`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        ideaVersion = """<idea-version since-build="2018.1"/>"""
      }, listOf(ErroneousSinceBuild("plugin.xml", IdeVersion.createIdeVersion("2018.1")))
    )

    `test invalid plugin xml`(
        perfectXmlBuilder.modify {
          ideaVersion = """<idea-version since-build="171.1" until-build="2018.*"/>"""
        }, listOf(ErroneousUntilBuild("plugin.xml", IdeVersion.createIdeVersion("2018.*")))
    )
  }

  @Test
  fun `since or until build with productCode`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        ideaVersion = """<idea-version since-build="IU-183.0"/>"""
      }, listOf(ProductCodePrefixInBuild("plugin.xml"))
    )

    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        ideaVersion = """<idea-version since-build="171.1" until-build="IU-183.*"/>"""
      }, listOf(ProductCodePrefixInBuild("plugin.xml"))
    )
  }

  @Test
  fun `invalid product descriptor`() {
    `test invalid plugin xml`(
        perfectXmlBuilder.modify {
          productDescriptor = """<product-descriptor/>"""
        },
        listOf(
            PropertyNotSpecified("code", "plugin.xml"),
            PropertyNotSpecified("release-date", "plugin.xml"),
        PropertyNotSpecified("release-version", "plugin.xml")
      )
    )

    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        productDescriptor = """<product-descriptor code="ABC" release-date="not-date" release-version="not-int"/>"""
      },
      listOf(
        ReleaseDateWrongFormat,
        NotNumber("release-version", "plugin.xml")
      )
    )
  }

  @Test
  fun `too long properties specified`() {

    val string65 = "a".repeat(65)
    val string256 = "a".repeat(256)
    val string65536 = "a".repeat(65536)

    val expectedProblems = listOf(
      TooLongPropertyValue("plugin.xml", "id", 256, 255),
      TooLongPropertyValue("plugin.xml", "name", 256, 255),
      TooLongPropertyValue("plugin.xml", "version", 65, 64),
      TooLongPropertyValue("plugin.xml", "plugin url", 256, 255),
      TooLongPropertyValue("plugin.xml", "vendor", 256, 255),
      TooLongPropertyValue("plugin.xml", "vendor email", 256, 255),
      TooLongPropertyValue("plugin.xml", "vendor url", 256, 255),
      TooLongPropertyValue("plugin.xml", "description", 65536, 65535),
      TooLongPropertyValue("plugin.xml", "<change-notes>", 65536, 65535)
    )

    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        ideaPluginTagOpen = """<idea-plugin url="$string256">"""
        id = "<id>$string256</id>"
        name = "<name>$string256</name>"
        version = "<version>$string65</version>"
        vendor = """<vendor email="$string256" url="$string256">$string256</vendor>"""
        description = "<description>$string65536</description>"
        changeNotes = "<change-notes>$string65536</change-notes>"
      }, expectedProblems
    )
  }

  @Test
  fun `application listeners are available only since 193`() {
    `test plugin xml warnings`(
      perfectXmlBuilder.modify {
        ideaVersion = """<idea-version since-build="181.1" until-build="193.1"/>"""
        additionalContent = """
            <applicationListeners>
              <listener class="SomeClass" topic="SomeTopic"/>            
            </applicationListeners>
          """.trimIndent()
      },
      listOf(
        ElementAvailableOnlySinceNewerVersion(
          "applicationListeners",
          IdeVersion.createIdeVersion("193"),
          IdeVersion.createIdeVersion("181.1"),
          IdeVersion.createIdeVersion("193.1")
        )
      )
    )
  }

  @Test
  fun `project listeners are available only since 193`() {
    `test plugin xml warnings`(
      perfectXmlBuilder.modify {
        ideaVersion = """<idea-version since-build="181.1" until-build="193.1"/>"""
        additionalContent = """
            <projectListeners>
              <listener class="SomeClass" topic="SomeTopic"/>            
            </projectListeners>
          """.trimIndent()
      },
      listOf(
        ElementAvailableOnlySinceNewerVersion(
          "projectListeners",
          IdeVersion.createIdeVersion("193"),
          IdeVersion.createIdeVersion("181.1"),
          IdeVersion.createIdeVersion("193.1")
        )
      )
    )
  }

  @Test
  fun `application listener missing attributes`() {
    `test plugin xml warnings`(
      perfectXmlBuilder.modify {
        ideaVersion = """<idea-version since-build="193.1"/>"""
        additionalContent = """
            <applicationListeners>
              <listener irrelevantAttribute="42"/>            
            </applicationListeners>
          """.trimIndent()
      },
      listOf(
        ElementMissingAttribute("listener", "class"),
        ElementMissingAttribute("listener", "topic")
      )
    )
  }

  @Test
  fun `project listener missing attributes`() {
    `test plugin xml warnings`(
      perfectXmlBuilder.modify {
        ideaVersion = """<idea-version since-build="193.1"/>"""
        additionalContent = """
            <projectListeners>
              <listener irrelevantAttribute="42"/>            
            </projectListeners>
          """.trimIndent()
      },
      listOf(
        ElementMissingAttribute("listener", "class"),
        ElementMissingAttribute("listener", "topic")
      )
    )
  }

  @Test
  fun `plugin has cycle in optional plugin dependencies configuration files`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.jar")) {
      dir("META-INF") {
        file("plugin.xml") {
          perfectXmlBuilder.modify {
            depends += """<depends optional="true" config-file="a.xml">a</depends>"""
          }
        }

        file(
          "a.xml",
          """
                <idea-plugin>
                  <depends optional="true" config-file="b.xml">b</depends>
                </idea-plugin>
              """.trimIndent()
        )

        file(
          "b.xml",
          """
                <idea-plugin>
                  <depends optional="true" config-file="a.xml">b</depends>
                </idea-plugin>
              """.trimIndent()
        )
      }
    }
    assertExpectedProblems(pluginFile, listOf(OptionalDependencyDescriptorCycleProblem("plugin.xml", listOf("plugin.xml", "a.xml", "b.xml", "a.xml"))))
  }

}
