package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.implementation.ExceptionMethod
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NoExplicitDependencyOnJavaPluginTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  /**
   * Plugin uses class from Java plugin but does not declare <depends>com.intellij.modules.java</depends>
   *
   * IDEA
   *    Java-plugin
   *        ```class javaPlugin.JavaPluginException : RuntimeException() { ... }```
   *
   * Plugin
   * ```
   *    public class usage.Usage {
   *       public void foo() {
   *          throw new javaPlugin.JavaPluginException()
   *       }
   *    }
   * ```
   */
  @Test
  fun `plugin uses classes of Java plugin but does not declare dependency onto it`() {
    // Build class files

    val javaPluginException = ByteBuddy()
        .subclass(RuntimeException::class.java)
        .name("javaPlugin.JavaPluginException")
        .make()

    val usageClass = ByteBuddy()
        .subclass(Any::class.java)
        .name("usage.Usage")
        .defineMethod("method", Void.TYPE, Visibility.PUBLIC)
        .intercept(ExceptionMethod.throwing(javaPluginException.typeDescription))
        .make()

    // Build mock plugin

    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.jar")) {
      dir("usage") {
        file("Usage.class", usageClass.bytes)
      }

      dir("META-INF") {
        file("plugin.xml") {
          """
          <idea-plugin>
            <id>someId</id>
            <name>someName</name>
            <version>someVersion</version>
            ""<vendor email="vendor.com" url="url">vendor</vendor>""
            <description>this description is looooooooooong enough</description>
            <change-notes>these change-notes are looooooooooong enough</change-notes>
            <idea-version since-build="131.1"/>
          </idea-plugin>
          """.trimIndent()
        }
      }
    }

    val idePlugin = (IdePluginManager.createManager().createPlugin(pluginFile) as PluginCreationSuccess).plugin

    // Build mock IDE

    val ideaDirectory = buildDirectory(temporaryFolder.newFolder("idea")) {
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
              </idea-plugin>
              """.trimIndent()
            }
          }
        }
      }
      dir("plugins") {
        dir("java") {
          dir("lib") {
            zip("java.jar") {
              dir("META-INF") {
                file("plugin.xml") {
                  """
                  <idea-plugin>
                    <id>com.intellij.java</id>
                    <module value="com.intellij.modules.java"/>
                  </idea-plugin>
                  """.trimIndent()
                }
              }

              dir("javaPlugin") {
                file("JavaPluginException.class", javaPluginException.bytes)
              }
            }
          }
        }
      }
    }

    // Fast assert IDE is fine

    val ide = IdeManager.createManager().createIde(ideaDirectory)
    assertEquals("IU-192.1", ide.version.asString())

    val javaPlugin = ide.bundledPlugins.find { it.pluginId == "com.intellij.java" }!!
    assertEquals("com.intellij.java", javaPlugin.pluginId)
    assertEquals(setOf("com.intellij.modules.java"), javaPlugin.definedModules)

    // Run verification
    val verificationResult = VerificationRunner().runPluginVerification(ide, idePlugin) as PluginVerificationResult.Verified

    assertEquals(emptySet<CompatibilityProblem>(), verificationResult.compatibilityProblems)

    // Verify expected warning is emitted

    assertEquals(setOf(
        "Plugin uses class 'javaPlugin.JavaPluginException' at 'usage.Usage.method() : void' but does not declare explicit " +
            "dependency on Java plugin using <depends>com.intellij.modules.java</depends>. " +
            "Java functionality was extracted from IntelliJ Platform to a separate plugin in IDEA 2019.2. " +
            "For more info refer to https://blog.jetbrains.com/platform/2019/06/java-functionality-extracted-as-a-plugin"
    ), verificationResult.compatibilityWarnings.map { it.message }.toSet())
  }
}