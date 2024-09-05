package com.jetbrains.plugin.structure.intellij

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.problems.NoDependencies
import com.jetbrains.plugin.structure.intellij.problems.OptionalDependencyConfigFileIsEmpty
import com.jetbrains.plugin.structure.intellij.problems.OptionalDependencyConfigFileNotSpecified
import com.jetbrains.plugin.structure.intellij.problems.ReleaseVersionWrongFormat
import com.jetbrains.plugin.structure.intellij.problems.ServiceExtensionPointPreloadNotSupported
import com.jetbrains.plugin.structure.intellij.problems.SuspiciousReleaseVersion
import com.jetbrains.plugin.structure.intellij.version.ProductReleaseVersion
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

private const val HEADER = """
      <id>someId</id>
      <name>someName</name>
      <version>someVersion</version>
      ""<vendor email="vendor.com" url="url">vendor</vendor>""
      <description>this description is looooooooooong enough</description>
      <change-notes>these change-notes are looooooooooong enough</change-notes>
      <idea-version since-build="131.1"/>
    """

private const val HEADER_WITHOUT_VERSION = """
      <id>someId</id>
      <name>someName</name>
      <vendor email="vendor.com" url="url">vendor</vendor>
      <description>this description is looooooooooong enough</description>
      <change-notes>these change-notes are looooooooooong enough</change-notes>
      <idea-version since-build="131.1"/>
    """

class PluginXmlValidationTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `plugin declaring optional dependency but missing config-file`() {
    val pluginCreationSuccess = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER
              <depends>com.intellij.modules.platform</depends>
              <depends optional="true">com.intellij.bundled.plugin.id</depends>
            </idea-plugin>
          """
        }
      }
    }

    val warnings = pluginCreationSuccess.warnings
    assertEquals(1, warnings.size)
    val warning = warnings.filterIsInstance<OptionalDependencyConfigFileNotSpecified>()
            .singleOrNull()
    assertNotNull("Expected 'Optional Dependency Config File Not Specified' plugin warning", warning)
  }

  @Test
  fun `plugin declaring optional dependency but empty config-file is a creation error`() {
    val pluginCreationFail = buildMalformedPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER
              <depends>com.intellij.modules.platform</depends>
              <depends optional="true" config-file="">com.intellij.optional.plugin.id</depends>
            </idea-plugin>
          """
        }
      }
    }

    val errorsAndWarnings = pluginCreationFail.errorsAndWarnings
    assertEquals(1, errorsAndWarnings.size)
    val error = errorsAndWarnings.filterIsInstance<OptionalDependencyConfigFileIsEmpty>()
            .singleOrNull()
    assertNotNull("Expected 'Optional Dependency Config File Is Empty' plugin warning", error)
  }

  @Test
  fun `plugin declaring projectService with preloading should emit an unacceptable warning`() {
    val pluginCreationSuccess = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER
              <depends>com.intellij.modules.platform</depends>
              <extensions defaultExtensionNs="com.intellij">
                <applicationService
                    serviceInterface="com.example.MyAppService"
                    serviceImplementation="com.example.MyAppServiceImpl"
                    preload="await"
                    />
              </extensions>              
            </idea-plugin>
          """
        }
      }
    }

    val warnings = pluginCreationSuccess.allWarnings
    assertEquals(1, warnings.size)
    val error = warnings.filterIsInstance<ServiceExtensionPointPreloadNotSupported>()
      .singleOrNull()
    assertNotNull("Expected 'Service Extension Point Preload Not Supported' plugin error", error)
    assertEquals(PluginProblem.Level.UNACCEPTABLE_WARNING, error?.level)
  }

  @Test
  fun `non-v2 plugin without dependencies error`() {
    val pluginCreationFail = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER
            </idea-plugin>
          """
        }
      }
    }

    val unacceptableWarnings = pluginCreationFail.unacceptableWarnings
    assertEquals(1, unacceptableWarnings.size)
    val unacceptableWarning = unacceptableWarnings.filterIsInstance<NoDependencies>()
      .singleOrNull()
    assertNotNull("Plugin descriptor plugin.xml does not include any module dependency tags. The plugin is assumed to be a legacy plugin and is loaded only in IntelliJ IDEA. See https://plugins.jetbrains.com/docs/intellij/plugin-compatibility.html", unacceptableWarning)
  }

  @Test
  fun `paid plugin`() {
    val pluginCreationSuccess = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER_WITHOUT_VERSION
              <version>1.1</version>
              <product-descriptor code="PCODE" release-date="20240813" release-version="10"/>
              <depends>com.intellij.modules.lang</depends>              
            </idea-plugin>
          """
        }
      }
    }
    val plugin = pluginCreationSuccess.plugin
    assertNotNull(plugin.productDescriptor)
    with(plugin.productDescriptor!!) {
      assertEquals("PCODE", code)
      assertEquals(LocalDate.of(2024, 8, 13), releaseDate)
      assertEquals(ProductReleaseVersion(10), version)
    }
  }

  @Test
  fun `paid plugin with an incorrect single-digit release version`() {
    val creationResult = buildMalformedPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER_WITHOUT_VERSION
              <version>1.1</version>
              <product-descriptor code="PCODE" release-date="20240813" release-version="1"/>
              <depends>com.intellij.modules.lang</depends>              
            </idea-plugin>
          """
        }
      }
    }
    with(creationResult.errorsAndWarnings) {
      assertEquals(1, size)
      val problem = filterIsInstance<ReleaseVersionWrongFormat>()
        .singleOrNull()
      assertNotNull(problem)
      problem!!
      assertEquals("Invalid plugin descriptor 'plugin.xml'. The <release-version> parameter (1) format is invalid. Ensure it is an integer with at least two digits.", problem.toString())
    }
  }

  @Test
  fun `paid plugin with a mismatching release version and plugin version`() {
    val creationResult = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER_WITHOUT_VERSION
              <version>2.0</version>
              <product-descriptor code="PCODE" release-date="20240813" release-version="10"/>
              <depends>com.intellij.modules.lang</depends>              
            </idea-plugin>
          """
        }
      }
    }
    with(creationResult.warnings) {
      assertEquals(1, size)
      val problem = filterIsInstance<SuspiciousReleaseVersion>().singleOrNull()
      assertNotNull(problem)
      problem!!
      assertEquals("Invalid plugin descriptor 'plugin.xml'. The <release-version> parameter [10] and the plugin version [2.0] should have a matching beginning. For example, release version '20201' should match plugin version 2020.1.1", problem.toString())
    }
  }

  @Test
  fun `paid plugin with a mismatching release version and plugin version in minor components`() {
    val creationResult = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER_WITHOUT_VERSION
              <version>2020.2.1</version>
              <product-descriptor code="PCODE" release-date="20240813" release-version="20201"/>
              <depends>com.intellij.modules.lang</depends>              
            </idea-plugin>
          """
        }
      }
    }
    with(creationResult.warnings) {
      assertEquals(1, size)
      val problem = filterIsInstance<SuspiciousReleaseVersion>().singleOrNull()
      assertNotNull(problem)
      problem!!
      assertEquals("Invalid plugin descriptor 'plugin.xml'. The <release-version> parameter [20201] and the plugin version [2020.2.1] should have a matching beginning. For example, release version '20201' should match plugin version 2020.1.1", problem.toString())
    }
  }

  private fun buildMalformedPlugin(pluginContentBuilder: ContentBuilder.() -> Unit): PluginCreationFail<IdePlugin> {
    val pluginCreationResult = buildIdePlugin(pluginContentBuilder)
    if (pluginCreationResult !is PluginCreationFail) {
      fail("This plugin was expected to fail during creation, but the creation process was successful." +
              " Please ensure that this is the intended behavior in the unit test.")
    }
    return pluginCreationResult as PluginCreationFail
  }

  private fun buildCorrectPlugin(pluginContentBuilder: ContentBuilder.() -> Unit): PluginCreationSuccess<IdePlugin> {
    val pluginCreationResult = buildIdePlugin(pluginContentBuilder)
    if (pluginCreationResult !is PluginCreationSuccess) {
      fail("This plugin has not been created. Creation failed with error(s).")
    }
    return pluginCreationResult as PluginCreationSuccess
  }

  private fun buildIdePlugin(pluginContentBuilder: ContentBuilder.() -> Unit): PluginCreationResult<IdePlugin> {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.jar").toPath(), pluginContentBuilder)
    return IdePluginManager.createManager().createPlugin(pluginFile)
  }

  val PluginCreationSuccess<IdePlugin>.allWarnings
    get() = warnings + unacceptableWarnings
}