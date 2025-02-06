package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.telemetry.PLUGIN_VERIFICATION_TIME
import com.jetbrains.plugin.structure.base.telemetry.PLUGIN_VERIFIED_CLASSES_COUNT
import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginInfo
import com.jetbrains.pluginverifier.runSeveralVerifiers
import com.jetbrains.pluginverifier.tests.mocks.TelemetryVerificationReportage
import net.bytebuddy.ByteBuddy
import net.bytebuddy.dynamic.DynamicType
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Duration

class PluginVerifierTelemetryTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  companion object {

    private const val HEADER = """
      <id>someId</id>
      <name>someName</name>
      <version>someVersion</version>
      ""<vendor email="vendor.com" url="url">vendor</vendor>""
      <description>this description is looooooooooong enough</description>
      <change-notes>these change-notes are looooooooooong enough</change-notes>
      <idea-version since-build="131.1"/>
    """

  }

  @Test
  fun `plugin be checked and telemetry can be gathered`() {
    checkPlugin(
      buildPluginWithXml {
        """
          <idea-plugin>
            $HEADER
          </idea-plugin>
        """
      }
    ).let {
      val pluginVerificationDuration = it.telemetry[PLUGIN_VERIFICATION_TIME]
      if (pluginVerificationDuration !is Duration) {
        fail("Plugin telemetry must contain the '$PLUGIN_VERIFICATION_TIME' value")
      } else {
        assertTrue(pluginVerificationDuration.toMillis() >= 0)
      }
    }
  }

  @Test
  fun `plugin verification classes`() {
    val pluginClasses = ByteBuddy()
      .subclass(Any::class.java)
      .name("javaPlugin.JavaClass")
      .make()

    checkPlugin(
      buildPluginWithXml(pluginClasses) {
        """
          <idea-plugin>
            $HEADER
          </idea-plugin>
        """
      }
    ).let {
      val pluginVerificationDuration = it.telemetry[PLUGIN_VERIFICATION_TIME]
      if (pluginVerificationDuration !is Duration) {
        fail("Plugin telemetry must contain the '$PLUGIN_VERIFICATION_TIME' value")
      } else {
        assertTrue(pluginVerificationDuration.toMillis() > 0)
      }
      val verifiedClassCount = it.telemetry[PLUGIN_VERIFIED_CLASSES_COUNT]
      if (verifiedClassCount !is Int) {
        fail("Plugin telemetry must contain the '$PLUGIN_VERIFICATION_TIME' value as an integer, but found " + verifiedClassCount?.javaClass)
      } else {
        assertEquals(1, verifiedClassCount)
      }
    }
  }

  private fun checkPlugin(idePlugin: IdePlugin): MeasuredVerificationResult {
    val ide = buildIde()
    return runVerification(ide, idePlugin)
  }

  private fun runVerification(ide: Ide, idePlugin: IdePlugin): MeasuredVerificationResult {
    val reportage = TelemetryVerificationReportage()
    val verificationResult = VerificationRunner().withPluginVerifier(ide, idePlugin, pluginVerifierHandler = {
      val pluginVerificationResults = runSeveralVerifiers(reportage, listOf(it))
      pluginVerificationResults.first()
    })
    val telemetry = reportage[LocalPluginInfo(idePlugin)] ?: PluginTelemetry()
    return MeasuredVerificationResult(verificationResult, telemetry)
  }

  private fun buildPluginWithXml(classContent: DynamicType.Unloaded<Any>? = null, pluginXmlContent: () -> String): IdePlugin {
    return buildPlugin {
      dir("META-INF") {
        file("plugin.xml", pluginXmlContent())
      }
      classContent?.let {
        clazz(it)
      }
    }
  }

  private fun buildPlugin(pluginContentBuilder: ContentBuilder.() -> Unit): IdePlugin {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.jar").toPath(), pluginContentBuilder)
    val pluginCreationResult = IdePluginManager.createManager().createPlugin(pluginFile)
    if (pluginCreationResult is PluginCreationFail) {
      fail(pluginCreationResult.errorsAndWarnings.joinToString { it.message })
    }
    return (pluginCreationResult as PluginCreationSuccess).plugin
  }

  private fun buildIde(): Ide {
    val ideaDirectory = buildDirectory(temporaryFolder.newFolder("idea").toPath()) {
      file("build.txt", "IU-192.1")
      dir("lib") {
        zip("idea.jar") {
          dir("META-INF") {
            file("plugin.xml") {
              """
                <idea-plugin>
                  <id>com.intellij</id>
                  <name>IDEA CORE</name>
                  <version>1.0</version>
                  <module value="com.intellij.modules.all"/>                
                  
                  <extensionPoints>
                    <extensionPoint name="nonDynamicEP" interface="doesntMatter"/>
                    <extensionPoint name="dynamicEP" interface="doesntMatter" dynamic="true"/>
                  </extensionPoints>                
                </idea-plugin>
                """.trimIndent()
            }
          }
        }
      }

      dir("plugins") {
        dir("bundled") {
          zip("bundled-plugin.zip") {
            dir("META-INF") {
              file("plugin.xml") {
                """
                <idea-plugin>
                  <id>com.intellij.bundled.plugin.id</id>
                  <name>Bundled plugin</name>
                  <version>1.0</version>            
                </idea-plugin>
                """.trimIndent()
              }
            }
          }
        }
      }
    }

    val ide = IdeManager.createManager().createIde(ideaDirectory)
    assertEquals("IU-192.1", ide.version.asString())
    return ide
  }

  private data class MeasuredVerificationResult(val verificationResult: PluginVerificationResult,
                                                val telemetry: PluginTelemetry)

  private fun ContentBuilder.clazz(cls: DynamicType.Unloaded<Any>) {
    val clsDesc = cls.typeDescription
    clsDesc.`package`?.let { pkg ->
      dir(pkg.name) {
        file(clsDesc.simpleName + ".class", cls.bytes)
      }
    }
  }
}