package com.jetbrains.pluginverifier.tests.dependencies

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.tests.BasePluginTest
import com.jetbrains.pluginverifier.tests.VerificationRunner
import com.jetbrains.pluginverifier.tests.mocks.buildIdePlugin
import org.junit.Test

/**
 * Reproduces the false-positive dependency resolution reported in
 * [MP-3742](https://youtrack.jetbrains.com/issue/MP-3742): a plugin whose content module
 * (e.g. `intellij.selenium.docker`) is misreported as an unresolved "missing mandatory
 * dependency" because content-module descriptors were not fed into [IdePlugin.definedModules],
 * and a `com.intellij.modules.*` marker exposed only via an IDE content module (e.g.
 * `com.intellij.modules.ultimate`) was likewise unresolvable.
 *
 * Both cases were fixed by "Support content modules descriptors" (843606da4, 2023-02-21) and
 * hardened by later work (e.g. #1391, #1366, #1347). This test guards against a regression.
 */
class ContentModuleDependencyResolutionTest : BasePluginTest() {

  private fun buildCoreIdeWithContentModule(): Ide {
    val ideaDirectory = buildDirectory(temporaryFolder.newFolder("idea").toPath()) {
      file("build.txt", "IU-213.1")
      dir("lib") {
        zip("idea.jar") {
          dir("META-INF") {
            file("plugin.xml") {
              """
                <idea-plugin>
                  <id>com.intellij</id>
                  <name>IDEA CORE</name>
                  <version>1.0</version>
                  <module value="com.intellij.modules.platform"/>
                  <content>
                    <module name="intellij.platform.ultimate"/>
                  </content>
                </idea-plugin>
                """.trimIndent()
            }
          }
          file("intellij.platform.ultimate.xml") {
            """
              <idea-plugin>
                <module value="com.intellij.modules.ultimate"/>
              </idea-plugin>
              """.trimIndent()
          }
        }
      }
    }
    return IdeManager.createManager().createIde(ideaDirectory)
  }

  private fun buildSeleniumLikePlugin(): IdePlugin =
    pluginJarPath.buildIdePlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              <id>SeleniumUITesting</id>
              <name>Selenium UI Testing</name>
              <version>1.0</version>
              <vendor email="vendor.com" url="url">vendor</vendor>
              <description>this description is looooooooooong enough</description>
              <change-notes>these change-notes are looooooooooong enough</change-notes>
              <idea-version since-build="213.1" until-build="231.1"/>
              <depends>com.intellij.modules.ultimate</depends>
              <content>
                <module name="intellij.selenium.docker"/>
              </content>
              <dependencies>
                <module name="intellij.selenium.docker"/>
              </dependencies>
            </idea-plugin>
            """.trimIndent()
        }
      }
      file("intellij.selenium.docker.xml") {
        """
          <idea-plugin>
          </idea-plugin>
          """.trimIndent()
      }
    }

  @Test
  fun `plugin's own content module and IDE content-module-exposed synthetic module both resolve`() {
    val ide = buildCoreIdeWithContentModule()
    val plugin = buildSeleniumLikePlugin()

    val verificationResult = VerificationRunner().runPluginVerification(ide, plugin)
    check(verificationResult is PluginVerificationResult.Verified) {
      "Expected Verified result, got $verificationResult"
    }
    assertEmpty("Compatibility problems", verificationResult.compatibilityProblems)
    assertEmpty("Direct Missing Mandatory Dependencies", verificationResult.directMissingMandatoryDependencies)
  }
}
