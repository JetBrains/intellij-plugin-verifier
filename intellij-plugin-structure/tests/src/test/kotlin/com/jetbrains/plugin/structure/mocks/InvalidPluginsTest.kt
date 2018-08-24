package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.problems.UnableToExtractZip
import com.jetbrains.plugin.structure.base.problems.UnexpectedDescriptorElements
import com.jetbrains.plugin.structure.base.utils.archiveDirectory
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.problems.*
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder
import java.io.File
import java.lang.IllegalArgumentException

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
      assertThat(actualProblems, containsInAnyOrder(*expectedProblems.toTypedArray()))
      assertThat(actualProblems, hasSize(expectedProblems.size))
    }

    private fun getFailedResult(pluginFile: File): PluginCreationFail<IdePlugin> {
      val pluginCreationResult = IdePluginManager.createManager().createPlugin(pluginFile)
      assertThat(pluginCreationResult, instanceOf(PluginCreationFail::class.java))
      return pluginCreationResult as PluginCreationFail
    }

    private fun getSuccessResult(pluginFile: File): PluginCreationSuccess<IdePlugin> {
      val pluginCreationResult = IdePluginManager.createManager().createPlugin(pluginFile)
      assertThat(pluginCreationResult, instanceOf(PluginCreationSuccess::class.java))
      return pluginCreationResult as PluginCreationSuccess<IdePlugin>
    }
  }

  @Test
  fun `incorrect plugin file type`() {
    val incorrect = temporaryFolder.newFile("incorrect.txt")
    assertExpectedProblems(incorrect, listOf(IncorrectIntellijFile(incorrect.name)))
  }

  @Test
  fun `failed to read jar file`() {
    val incorrect = temporaryFolder.newFile("incorrect.jar")
    assertExpectedProblems(incorrect, listOf(UnableToReadJarFile()))
  }

  @Test()
  fun `plugin file does not exist`() {
    val nonExistentFile = File("non-existent-file")
    expectedEx.expect(IllegalArgumentException::class.java)
    expectedEx.expectMessage("Plugin file non-existent-file does not exist")
    IdePluginManager.createManager().createPlugin(nonExistentFile)
  }

  @Test
  fun `unable to extract plugin`() {
    val brokenZipArchive = temporaryFolder.newFile("broken.zip")
    assertExpectedProblems(brokenZipArchive, listOf(UnableToExtractZip()))
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
        listOf(PropertyNotSpecified("description", "plugin.xml")))
  }

  @Test
  fun `plugin name is not specified`() {
    `test invalid plugin xml`(
        perfectXmlBuilder.modify {
          name = ""
        },
        listOf(PropertyNotSpecified("name", "plugin.xml")))
  }

  @Test
  fun `plugin id is not specified but it is equal to name`() {
    val pluginXmlContent = perfectXmlBuilder.modify {
      id = ""
    }
    val pluginFolder = getTempPluginFolder(pluginXmlContent)
    val successResult = getSuccessResult(pluginFolder)
    val plugin = successResult.plugin
    assertThat(plugin.pluginId, `is`(plugin.pluginName))
  }

  @Test
  fun `plugin vendor is not specified`() {
    `test invalid plugin xml`(
        perfectXmlBuilder.modify {
          vendor = ""
        },
        listOf(PropertyNotSpecified("vendor", "plugin.xml")))
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
        listOf(PropertyNotSpecified("idea-version", "plugin.xml")))
  }

  @Test
  fun `invalid dependency bean`() {
    `test invalid plugin xml`(
        perfectXmlBuilder.modify {
          depends = listOf("")
        },
        listOf(InvalidDependencyBean("plugin.xml")))
  }

  @Test
  fun `invalid module bean`() {
    `test invalid plugin xml`(
        perfectXmlBuilder.modify {
          modules = listOf("")
        },
        listOf(InvalidModuleBean("plugin.xml")))
  }

  @Test
  fun `missing since build`() {
    `test invalid plugin xml`(
        perfectXmlBuilder.modify {
          ideaVersion = "<idea-version/>"
        },
        listOf(SinceBuildNotSpecified("plugin.xml")))
  }

  @Test
  fun `invalid since build`() {
    `test invalid plugin xml`(
        perfectXmlBuilder.modify {
          ideaVersion = """<idea-version since-build="131."/>"""
        },
        listOf(InvalidSinceBuild("plugin.xml", "131.")))
  }

  @Test
  fun `invalid until build`() {
    `test invalid plugin xml`(
        perfectXmlBuilder.modify {
          ideaVersion = """<idea-version since-build="131.1" until-build="141."/>"""
        },
        listOf(InvalidUntilBuild("plugin.xml", "141.")))
  }

  @Test
  fun `since build less then until `() {
    `test invalid plugin xml`(
        perfectXmlBuilder.modify {
          ideaVersion = """<idea-version since-build="131.1" until-build="120.1"/>"""
        },
        listOf(SinceBuildGreaterThanUntilBuild("plugin.xml", IdeVersion.createIdeVersion("131.1"), IdeVersion.createIdeVersion("120.1"))))
  }

  @Test
  fun `empty vendor`() {
    `test invalid plugin xml`(
        perfectXmlBuilder.modify {
          vendor = """<vendor></vendor>"""
        },
        listOf(PropertyNotSpecified("vendor", "plugin.xml")))

    `test valid plugin xml`(
        perfectXmlBuilder.modify {
          vendor = """<vendor>Vendor name</vendor>"""
        })

    `test valid plugin xml`(
        perfectXmlBuilder.modify {
          vendor = """<vendor url="http://vendor.url"></vendor>"""
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
        listOf(InvalidSinceBuild("plugin.xml", "129.0.*")))
  }

  @Test
  fun `non latin description`() {
    `test plugin xml warnings`(
        perfectXmlBuilder.modify {
          description = "<description>Описание без английского, но достаточно длинное</description>"
        },
        listOf(NonLatinDescription()))
  }

  @Test
  fun `html description`() {
    `test plugin xml warnings`(
        perfectXmlBuilder.modify {
          description = """<description><![CDATA[
          <a href=\"https://github.com/myamazinguserprofile/myamazingproject\">short text</a>
          ]]></description>"""
        },
        listOf(ShortDescription()))
  }

  @Test
  fun `default values`() {
    `test invalid plugin xml`("""<idea-plugin>
      <id>com.your.company.unique.plugin.id</id>
      <name>Plugin display name here</name>
      <version>1.0</version>
      <vendor email="support@yourcompany.com" url="http://www.yourcompany.com">YourCompany</vendor>
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
        PropertyWithDefaultValue("plugin.xml", "id"),
        PropertyWithDefaultValue("plugin.xml", "name"),
        DefaultDescription("plugin.xml"),
        DefaultChangeNotes("plugin.xml"),
        PropertyWithDefaultValue("plugin.xml", "vendor"),
        PropertyWithDefaultValue("plugin.xml", "vendor url"),
        PropertyWithDefaultValue("plugin.xml", "vendor email")
    ))
  }

  private fun `test plugin xml warnings`(pluginXmlContent: String, expectedWarnings: List<PluginProblem>) {
    val pluginFolder = getTempPluginFolder(pluginXmlContent)
    val successResult = getSuccessResult(pluginFolder)
    assertThat(successResult.warnings, `is`(expectedWarnings))
  }

  private fun `test invalid plugin xml`(pluginXmlContent: String, expectedProblems: List<PluginProblem>) {
    val pluginFolder = getTempPluginFolder(pluginXmlContent)
    assertExpectedProblems(pluginFolder, expectedProblems)
  }

  private fun `test valid plugin xml`(pluginXmlContent: String) {
    val pluginFolder = getTempPluginFolder(pluginXmlContent)
    getSuccessResult(pluginFolder)
  }

  private fun getTempPluginFolder(pluginXmlContent: String): File {
    val pluginFolder = temporaryFolder.newFolder()
    val metaInf = File(pluginFolder, "META-INF")
    metaInf.mkdirs()
    File(metaInf, "plugin.xml").writeText(pluginXmlContent)
    return pluginFolder
  }

  @Test
  fun `plugin has multiple plugin descriptors in lib directory where descriptor might miss mandatory elements`() {
    /*
      plugin/
      plugin/lib
      plugin/lib/one.jar!/META-INF/plugin.xml
      plugin/lib/two.jar!/META-INF/plugin.xml
    */
    val validPluginXmlOne = perfectXmlBuilder.modify {
      id = """<id>one</id>"""
      name = "<name>one</name>"
      version = "<version>one</version>"
    }
    val invalidPluginXmlOne = perfectXmlBuilder.modify {
      version = ""
    }

    val firstDescriptors = listOf(validPluginXmlOne, invalidPluginXmlOne)

    val validPluginXmlTwo = perfectXmlBuilder.modify {
      id = """<id>two</id>"""
      name = """<name>two</name>"""
      version = """<version>two</version>"""
    }
    val invalidPluginXmlTwo = perfectXmlBuilder.modify {
      version = ""
    }
    val secondDescriptors = listOf(validPluginXmlTwo, invalidPluginXmlTwo)

    var testNumber = 0
    for (firstDescriptor in firstDescriptors) {
      for (secondDescriptor in secondDescriptors) {
        testNumber++
        val pluginFolder = temporaryFolder.newFolder(testNumber.toString())
        val lib = File(pluginFolder, "lib")
        lib.mkdirs()

        val oneMetaInf = temporaryFolder.newFolder("one$testNumber", "META-INF")
        File(oneMetaInf, "plugin.xml").writeText(firstDescriptor)
        archiveDirectory(oneMetaInf, File(lib, "one.jar"))

        val twoMetaInf = temporaryFolder.newFolder("two$testNumber", "META-INF")
        File(twoMetaInf, "plugin.xml").writeText(secondDescriptor)
        archiveDirectory(twoMetaInf, File(lib, "two.jar"))

        assertExpectedProblems(pluginFolder, listOf(MultiplePluginDescriptorsInLibDirectory("one.jar", "two.jar")))
      }
    }
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
        }, listOf(UnresolvedXIncludeElements("plugin.xml"))
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
  fun `too long properties specified`() {

    val string_65 = "a".repeat(65)
    val string_256 = "a".repeat(256)
    val string_65536 = "a".repeat(65536)

    val expectedProblems = listOf(
        TooLongPropertyValue("plugin.xml", "version", 65, 64),
        TooLongPropertyValue("plugin.xml", "plugin url", 256, 255),
        TooLongPropertyValue("plugin.xml", "id", 256, 255),
        TooLongPropertyValue("plugin.xml", "name", 256, 255),
        TooLongPropertyValue("plugin.xml", "vendor", 256, 255),
        TooLongPropertyValue("plugin.xml", "vendor email", 256, 255),
        TooLongPropertyValue("plugin.xml", "vendor url", 256, 255),
        TooLongPropertyValue("plugin.xml", "description", 65536, 65535),
        TooLongPropertyValue("plugin.xml", "<change-notes>", 65536, 65535)
    )

    `test invalid plugin xml`(
        perfectXmlBuilder.modify {
          ideaPluginTagOpen = """<idea-plugin url="$string_256">"""
          id = "<id>$string_256</id>"
          name = "<name>$string_256</name>"
          version = "<version>$string_65</version>"
          vendor = """<vendor email="$string_256" url="$string_256">$string_256</vendor>"""
          description = "<description>$string_65536</description>"
          changeNotes = "<change-notes>$string_65536</change-notes>"
        }, expectedProblems
    )
  }
}
