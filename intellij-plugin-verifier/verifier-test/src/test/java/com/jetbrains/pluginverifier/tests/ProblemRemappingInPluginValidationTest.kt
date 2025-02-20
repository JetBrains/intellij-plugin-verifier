package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.problems.IntelliJPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.JetBrainsPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.ReleaseVersionWrongFormat
import com.jetbrains.plugin.structure.intellij.problems.remapping.JsonUrlProblemLevelRemappingManager
import com.jetbrains.plugin.structure.intellij.problems.remapping.RemappingSet
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ProblemRemappingInPluginValidationTest : BasePluginTest() {
  private lateinit var problemRemapper: JetBrainsPluginCreationResultResolver

  @Before
  fun setUp() {
    val levelRemapping = JsonUrlProblemLevelRemappingManager
      .fromClassPathJson()
      .getLevelRemapping(RemappingSet.JETBRAINS_PLUGIN_REMAPPING_SET)
    problemRemapper = JetBrainsPluginCreationResultResolver(IntelliJPluginCreationResultResolver(), levelRemapping)
  }

  @Test
  fun `plugin with JetBrains vendor is invalid because it misses the description`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.jar").toPath()) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin package="com.intellij.grazie.pro" require-restart="true">
              <idea-version since-build="242.20224" />
              <product-descriptor code="GZL" release-date="20240626" release-version="03" optional="true" />
              <version>0.3.359</version>
              <id>com.intellij.grazie.pro</id>
              <name>Grazie Pro</name>
              <vendor>JetBrains</vendor>
              <dependencies>
                <module name="intellij.dev.psiViewer" />
                <plugin id="org.intellij.plugins.markdown" />
              </dependencies>
            </idea-plugin>
          """
        }
      }
    }
    val pluginResult = IdePluginManager.createManager().createPlugin(pluginFile, validateDescriptor = true, problemResolver = problemRemapper)
    pluginResult.assertContains<PropertyNotSpecified>("Invalid plugin descriptor 'plugin.xml'. The property <description> is not specified.")
    pluginResult.assertContainsWarning<ReleaseVersionWrongFormat>("Invalid plugin descriptor 'plugin.xml'. " +
      "The <release-version> parameter (03) format is invalid. " +
      "Ensure it is an integer with at least two digits.")
  }

  @Test
  fun `plugin with JetBrains vendor is valid because plugin problems is remapped according to JSON rules`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.jar").toPath()) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin package="com.intellij.grazie.pro" require-restart="true">
              <idea-version since-build="242.20224" />
              <product-descriptor code="GZL" release-date="20240626" release-version="03" optional="true" />
              <version>0.3.359</version>
              <id>com.intellij.grazie.pro</id>
              <name>Grazie Pro</name>
              <vendor>JetBrains</vendor>
              <dependencies>
                <module name="intellij.dev.psiViewer" />
                <plugin id="org.intellij.plugins.markdown" />
              </dependencies>
              <description>
                Enhances the bundled Grazie Lite plugin with advanced natural language writing assistance
                Grazie Pro is a result of the latest developments in deep learning and natural language processing.
                By default, it processes everything locally, keeping your data private and secure,
                and provides the following features for English text
              </description>                            
            </idea-plugin>
          """
        }
      }
    }
    val pluginResult = IdePluginManager.createManager().createPlugin(pluginFile, validateDescriptor = true, problemResolver = problemRemapper)
    assertSuccess(pluginResult) {
      assertEmpty("Structure Unacceptable Warnings", unacceptableWarnings)
      assertEquals(1, warnings.size)
      assertContainsWarning<ReleaseVersionWrongFormat>("Invalid plugin descriptor 'plugin.xml'. " +
        "The <release-version> parameter (03) format is invalid. " +
        "Ensure it is an integer with at least two digits.")
    }
  }
}