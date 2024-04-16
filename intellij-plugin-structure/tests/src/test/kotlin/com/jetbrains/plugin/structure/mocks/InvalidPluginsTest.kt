package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.ContainsNewlines
import com.jetbrains.plugin.structure.base.problems.IncorrectZipOrJarFile
import com.jetbrains.plugin.structure.base.problems.MultiplePluginDescriptors
import com.jetbrains.plugin.structure.base.problems.NotBoolean
import com.jetbrains.plugin.structure.base.problems.NotNumber
import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.problems.TooLongPropertyValue
import com.jetbrains.plugin.structure.base.problems.UnableToExtractZip
import com.jetbrains.plugin.structure.base.problems.UnexpectedDescriptorElements
import com.jetbrains.plugin.structure.base.problems.VendorCannotBeEmpty
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.problems.*
import com.jetbrains.plugin.structure.intellij.verifiers.PRODUCT_ID_RESTRICTED_WORDS
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Path
import java.nio.file.Paths

class InvalidPluginsTest(fileSystemType: FileSystemType) : BasePluginManagerTest<IdePlugin, IdePluginManager>(fileSystemType) {
  private val DEFAULT_TEMPLATE_NAMES = setOf("Plugin display name here", "My Framework Support", "Template", "Demo")
  private val PLUGIN_NAME_RESTRICTED_WORDS = setOf(
    "plugin", "JetBrains", "IDEA", "PyCharm", "CLion", "AppCode", "DataGrip", "Fleet", "GoLand", "PhpStorm", "WebStorm",
    "Rider", "ReSharper", "TeamCity", "YouTrack", "RubyMine", "IntelliJ"
  )
  private val DEFAULT_TEMPLATE_DESCRIPTIONS = setOf(
    "Enter short description for your plugin here", "most HTML tags may be used", "example.com/my-framework"
  )

  override fun createManager(extractDirectory: Path): IdePluginManager =
    IdePluginManager.createManager(extractDirectory)

  @Test
  fun `incorrect plugin file type`() {
    val incorrect = temporaryFolder.newFile("incorrect.txt")
    assertProblematicPlugin(incorrect, listOf(IncorrectZipOrJarFile(incorrect.simpleName)))
  }

  @Test
  fun `failed to read jar file`() {
    val incorrect = temporaryFolder.newFile("incorrect.jar")
    assertProblematicPlugin(incorrect, listOf(UnableToExtractZip()))
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
    assertProblematicPlugin(brokenZipArchive, listOf(PluginZipIsEmpty()))
  }

  @Test
  fun `no meta-inf plugin xml found`() {
    val folder = temporaryFolder.newFolder()
    assertProblematicPlugin(folder, listOf(PluginDescriptorIsNotFound("plugin.xml")))
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
  fun `plugin id is empty`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        id = "<id></id>"
      },
      listOf(PropertyNotSpecified("id"))
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
  fun `plugin id has forbidden prefix`() {
    val warning = `test valid plugin xml`(
      perfectXmlBuilder.modify {
        id = "<id>com.example.plugin</id>"
      },
    ).warnings.single()
    assertEquals(ForbiddenPluginIdPrefix("com.example.plugin", "com.example"), warning)
  }

  @Test
  fun `plugin id has template word prefix`() {
    PRODUCT_ID_RESTRICTED_WORDS.forEach { product ->
      val warning = `test valid plugin xml`(
        perfectXmlBuilder.modify {
          id = "<id>plugin.${product}.improved</id>"
        }
      ).warnings.single()
      assertEquals(TemplateWordInPluginId("plugin.xml", product), warning)
    }
  }

  @Test
  fun `plugin name is not default`() {
    for (templateName in DEFAULT_TEMPLATE_NAMES) {
      `test invalid plugin xml`(
        perfectXmlBuilder.modify {
          name = "<name>${templateName}</name>"
        },
        listOf(PropertyWithDefaultValue("plugin.xml", PropertyWithDefaultValue.DefaultProperty.NAME, templateName))
      )
    }
  }

  @Test
  fun `plugin name contains template words`() {
    for (templateWord in PLUGIN_NAME_RESTRICTED_WORDS) {
      val warning = `test valid plugin xml`(
        perfectXmlBuilder.modify {
          name = "<name>bla ${templateWord}bla</name>"
        }
      ).warnings.single()
      assertEquals(TemplateWordInPluginName("plugin.xml", templateWord), warning)
    }
  }

  @Test
  fun `plugin description is not default`() {
    for (templateDesc in DEFAULT_TEMPLATE_DESCRIPTIONS) {
      val descriptionText = "description ${templateDesc}description description"
      `test invalid plugin xml`(
        perfectXmlBuilder.modify {
          description = "<description>$descriptionText</description>"
        },
        listOf(PropertyWithDefaultValue("plugin.xml", PropertyWithDefaultValue.DefaultProperty.DESCRIPTION, descriptionText))
      )
    }
  }

  @Test
  fun `plugin id is not specified but it is equal to name`() {
    val pluginXmlContent = perfectXmlBuilder.modify {
      id = ""
    }
    val pluginFolder = getTempPluginFolder(pluginXmlContent)
    val successResult = createPluginSuccessfully(pluginFolder)
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
  fun `plugin vendor name is not specified`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        vendor = "<vendor url=\"https://test.com\"></vendor>"
      },
      listOf(VendorCannotBeEmpty("plugin.xml"))
    )
  }

  @Test
  fun `plugin version is not specified`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        version = ""
      }, listOf(PropertyNotSpecified("version", "plugin.xml")))
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
  fun `multiple dependencies reuse a single config-file`() {
    `test valid plugin xml`(
      perfectXmlBuilder.modify {
        depends = """
                <depends config-file="shared.xml">com.jetbrains.module-one</depends>
                <depends config-file="shared.xml">com.jetbrains.module-two</depends>
              """.trimIndent()
      }
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
  fun `since build contains wildcard `() {
    val specifiedVersion = "131.*"
    `test plugin xml warnings`(
      perfectXmlBuilder.modify {
        ideaVersion = """<idea-version since-build="$specifiedVersion" until-build="233.*"/>"""
      },
      listOf(
        SinceBuildCannotContainWildcard("plugin.xml", IdeVersion.createIdeVersion(specifiedVersion))
      )
    )
  }


  @Test
  fun `empty vendor`() {
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        vendor = """<vendor></vendor>"""
      },
      listOf(VendorCannotBeEmpty("plugin.xml"))
    )

    `test valid plugin xml`(
      perfectXmlBuilder.modify {
        vendor = """<vendor>Vendor name</vendor>"""
      })

    `test valid plugin xml`(
      perfectXmlBuilder.modify {
        vendor = """<vendor url="https://vendor.url">vendor</vendor>"""
      })

    `test valid plugin xml`(
      perfectXmlBuilder.modify {
        vendor = """<vendor email="vendor@email.com">vendor</vendor>"""
      })
  }

  @Test
  fun `wildcard in old ide`() {
    val wildcardSinceBuild = "129.0.*"
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        ideaVersion = """<idea-version since-build="$wildcardSinceBuild"/>"""
      },
      listOf(
        InvalidSinceBuild("plugin.xml", wildcardSinceBuild),
        SinceBuildCannotContainWildcard("plugin.xml", IdeVersion.createIdeVersion(wildcardSinceBuild))
      )
    )
  }

  @Test
  fun `latin description`() {
    val desc = "Latin description, 1234 & ;,.!-–— () long enough"
    val plugin = `test valid plugin xml`(
      perfectXmlBuilder.modify {
        description = "<description>${desc.replace("&", "&amp;")}</description>"
      }
    )
    assertEquals(desc, plugin.plugin.description)
    assert(plugin.unacceptableWarnings.isEmpty()) {
      "Plugin with latin description has some unacceptable warnings: ${plugin.unacceptableWarnings}"
    }
    assert(plugin.warnings.isEmpty()) {
      "Plugin with latin description has some warnings: ${plugin.warnings}"
    }
  }

  @Test
  fun `short latin description`() {
    `test plugin xml unacceptable warnings`(
      perfectXmlBuilder.modify {
        description = "<description>Too short latin description bla-bla-bla</description>"
      },
      listOf(ShortOrNonLatinDescription())
    )
  }

  @Test
  fun `non latin description`() {
    `test plugin xml unacceptable warnings`(
      perfectXmlBuilder.modify {
        description = "<description>Описание без английского, но достаточно длинное</description>"
      },
      listOf(ShortOrNonLatinDescription())
    )
  }

  @Test
  fun `http links in description`() {
    val testLinks = listOf(
      Pair("a", "http://test_a.com"),
      Pair("link", "http://test_link.com"),
      Pair("img", "http://test.png")
    )
    val linksString = testLinks.joinToString(", ") {
      val tag = it.first
      val attr = when (tag) {
        "img" -> "src"
        else -> "href"
      }
      val link = it.second

      "<$tag $attr='$link'></$tag>"
    }

    val desc = """
      <![CDATA[
        Long enough test description with http links in it:
        $linksString
      ]]>
    """.trimIndent()
    `test plugin xml unacceptable warnings`(
      perfectXmlBuilder.modify {
        description = "<description>$desc</description>"
      },
      testLinks.map { HttpLinkInDescription(it.second) }
    )
  }

  @Test
  fun `https and relative links in description`() {
    val desc = """
      <![CDATA[
        Long enough test description with http links in it:
        <a href='https://test_a.com'></a> 
        <a href='/test'></a>
        <a href='test.php'></a> 
        <link href='https://test_a.com'></link> 
        <link href='/test'></link> 
        <link href='test.php'></link> 
        <img src='https://test.png'></img>
        <img src='/test.png'></img>
        <img src='test.png'></img>
      ]]>
    """.trimIndent()
    `test valid plugin xml`(
      perfectXmlBuilder.modify {
        description = "<description>$desc</description>"
      }
    )
  }

  @Test
  fun `html description`() {
    `test plugin xml unacceptable warnings`(
      perfectXmlBuilder.modify {
        description = """<description><![CDATA[
          <a href=\"https://github.com/myamazinguserprofile/myamazingproject\">short text</a>
          ]]></description>"""
      },
      listOf(ShortOrNonLatinDescription())
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
      PropertyWithDefaultValue("plugin.xml", PropertyWithDefaultValue.DefaultProperty.ID, "com.your.company.unique.plugin.id"),
      PropertyWithDefaultValue("plugin.xml", PropertyWithDefaultValue.DefaultProperty.NAME, "Plugin display name here"),
      PropertyWithDefaultValue(
        "plugin.xml",
        PropertyWithDefaultValue.DefaultProperty.DESCRIPTION,
        "Enter short description for your plugin here. most HTML tags may be used"
      ),
      DefaultChangeNotes("plugin.xml"),
      PropertyWithDefaultValue("plugin.xml", PropertyWithDefaultValue.DefaultProperty.VENDOR, "YourCompany"),
      PropertyWithDefaultValue("plugin.xml", PropertyWithDefaultValue.DefaultProperty.VENDOR_URL, "https://www.yourcompany.com"),
      PropertyWithDefaultValue("plugin.xml", PropertyWithDefaultValue.DefaultProperty.VENDOR_EMAIL, "support@yourcompany.com")
    )
    )
  }

  private fun `test plugin xml warnings`(pluginXmlContent: String, expectedWarnings: List<PluginProblem>) {
    val pluginFolder = getTempPluginFolder(pluginXmlContent)
    val successResult = createPluginSuccessfully(pluginFolder)
    assertEquals(expectedWarnings, successResult.warnings)
  }

  private fun `test plugin xml unacceptable warnings`(pluginXmlContent: String, expectedUnacceptableWarnings: List<PluginProblem>) {
    val pluginFolder = getTempPluginFolder(pluginXmlContent)
    val successResult = createPluginSuccessfully(pluginFolder)
    assertEquals(expectedUnacceptableWarnings, successResult.unacceptableWarnings)
  }

  private fun `test invalid plugin xml`(pluginXmlContent: String, expectedProblems: List<PluginProblem>) {
    val pluginFolder = getTempPluginFolder(pluginXmlContent)
    assertProblematicPlugin(pluginFolder, expectedProblems)
  }

  private fun `test valid plugin xml`(pluginXmlContent: String): PluginCreationSuccess<IdePlugin> {
    val pluginFolder = getTempPluginFolder(pluginXmlContent)
    return createPluginSuccessfully(pluginFolder)
  }

  private fun getTempPluginFolder(pluginXmlContent: String): Path {
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

    assertProblematicPlugin(pluginDirectory, listOf(MultiplePluginDescriptors("plugin.xml", "one.jar", "plugin.xml", "two.jar")))
  }

  @Test
  fun `completely invalid plugin descriptor`() {
    `test invalid plugin xml`(
      "abracadabra",
      listOf(UnexpectedDescriptorElements(1, "plugin.xml"))
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
      }, listOf(InvalidUntilBuild("plugin.xml", "2018.*", IdeVersion.createIdeVersion("2018.*")))
    )
  }

  @Test
  fun `until build is a 999`() {
    val suspiciousUntilBuild = "999"
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        ideaVersion = """<idea-version until-build="$suspiciousUntilBuild" since-build="241" />"""
      }, listOf(
            InvalidUntilBuildWithMagicNumber(PLUGIN_XML, suspiciousUntilBuild, "999"),
        )
    )
  }

  @Test
  fun `until build has a single-component`() {
    val suspiciousUntilBuild = "233"
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        ideaVersion = """<idea-version until-build="$suspiciousUntilBuild" since-build="231" />"""
      }, listOf(
          InvalidUntilBuildWithJustBranch(PLUGIN_XML, suspiciousUntilBuild))
    )
  }

  @Test
  fun `until build is a nonexistent single-component release`() {
    val suspiciousUntilBuild = "234"
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        ideaVersion = """<idea-version until-build="$suspiciousUntilBuild" since-build="231" />"""
      }, listOf(
        InvalidUntilBuildWithJustBranch(PLUGIN_XML, suspiciousUntilBuild),
        NonexistentReleaseInUntilBuild(suspiciousUntilBuild, "2023.4"),
      )
    )
  }

  @Test
  fun `until build is a nonexistent release`() {
    val suspiciousUntilBuild = "234.1"
    `test plugin xml warnings`(
      perfectXmlBuilder.modify {
        ideaVersion = """<idea-version until-build="$suspiciousUntilBuild" since-build="231" />"""
      }, listOf(NonexistentReleaseInUntilBuild(suspiciousUntilBuild, "2023.4"))
    )
  }

  @Test
  fun `until build is a 999 dot star`() {
    val suspiciousUntilBuild = "999.*"
    val magicNumber = "999"
    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        ideaVersion = """<idea-version until-build="$suspiciousUntilBuild" since-build="241" />"""
      }, listOf(InvalidUntilBuildWithMagicNumber(PLUGIN_XML, suspiciousUntilBuild, magicNumber))
    )
  }

  @Test
  fun `until build contains a magic number in the secondary components`() {
    val suspiciousUntilBuild = "231.999.123"
    `test valid plugin xml`(
      perfectXmlBuilder.modify {
        ideaVersion = """<idea-version until-build="$suspiciousUntilBuild" since-build="223" />"""
      }
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
        ReleaseDateWrongFormat("plugin.xml"),
        NotNumber("release-version", "plugin.xml")
      )
    )

    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        productDescriptor = """<product-descriptor code="ABC" release-date="20180118" release-version="12" eap="aaaa"/>"""
      },
      listOf(
        NotBoolean("eap", "plugin.xml")
      )
    )

    `test invalid plugin xml`(
      perfectXmlBuilder.modify {
        productDescriptor = """<product-descriptor code="ABC" release-date="20180118" release-version="12" optional="not-bool"/>"""
      },
      listOf(
        NotBoolean("optional", "plugin.xml")
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
      TooLongPropertyValue("plugin.xml", "name", 256, 64),
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
  fun `status bar widget factory extension must declare an ID`() {
    val implementation = "com.example.MyStatusBarWidgetFactory"
    `test plugin xml unacceptable warnings`(
      perfectXmlBuilder.modify {
        additionalContent = """
            <extensions defaultExtensionNs="com.intellij">
              <statusBarWidgetFactory implementation="$implementation" />
            </extensions>
        """.trimIndent()
      }, listOf(StatusBarWidgetFactoryExtensionPointIdMissing(implementation))
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
    assertProblematicPlugin(pluginFile, listOf(OptionalDependencyDescriptorCycleProblem("plugin.xml", listOf("plugin.xml", "a.xml", "b.xml", "a.xml"))))
  }

  @Test
  fun `optional dependency configuration file is invalid leads to warnings of the main plugin descriptor`() {
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
                  <depends></depends>
                </idea-plugin>
              """.trimIndent()
        )
      }
    }
    val allWarnings = createPluginSuccessfully(pluginFile).warnings
    val optionalDependenciesWarnings = allWarnings.filterIsInstance<OptionalDependencyDescriptorResolutionProblem>()
    assertEquals(allWarnings, optionalDependenciesWarnings)
    assertEquals(
      listOf(
        OptionalDependencyDescriptorResolutionProblem("a", "a.xml", listOf(InvalidDependencyId("a.xml", "")))
      ),
      optionalDependenciesWarnings
    )
  }

  @Test
  fun `plugin has unknown value in service client`() {
    `test plugin xml warnings`(
      perfectXmlBuilder.modify {
        additionalContent = """
              <extensions defaultExtensionNs="com.intellij">
                <applicationService
                    serviceInterface="com.example.MyAppService"
                    serviceImplementation="com.example.MyAppServiceImpl"
                    client="xxx"/>
              </extensions>
          """.trimIndent()
      },
      listOf(UnknownServiceClientValue("plugin.xml", "xxx"))
    )
  }
}
