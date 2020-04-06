package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.implementation.ExceptionMethod
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.matcher.ElementMatchers
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

    val idePlugin = buildIdePlugin {
      dir("usage") {
        file("Usage.class", usageClass.bytes)
      }
    }

    val ide = buildIdeWithBundledPlugins(
      {
        dir("javaPlugin") {
          file("JavaPluginException.class", javaPluginException.bytes)
        }
      },
      {}
    )

    // Run verification
    val verificationResult = VerificationRunner().runPluginVerification(ide, idePlugin) as PluginVerificationResult.Verified

    assertEquals(emptySet<CompatibilityProblem>(), verificationResult.compatibilityProblems)

    // Verify expected warning is emitted

    assertEquals(
      setOf(
        """
        Plugin uses classes of Java plugin, for example
        'javaPlugin.JavaPluginException' is used at 'usage.Usage.method() : void'
        but the plugin does not declare explicit dependency on the Java plugin, via <depends>com.intellij.modules.java</depends>. 
        Java functionality was extracted from the IntelliJ Platform to a separate plugin in IDEA 2019.2. 
        For more info refer to https://blog.jetbrains.com/platform/2019/06/java-functionality-extracted-as-a-plugin
        
      """.trimIndent()
      ), verificationResult.compatibilityWarnings.map { it.fullDescription }.toSet()
    )
  }

  /**
   * Plugin uses class from Groovy plugin (which depends on Java plugin) but does not use classes of Java plugin directly.
   * Verifier must not produce a warning in this case.
   *
   * IDEA
   *    Java-plugin
   *        ```class javaPlugin.JavaClass { ... }```
   *
   *    Groovy-plugin
   *        ```class groovyPlugin.GroovyClass extends javaPlugin.JavaClass { ... }```
   *
   * Plugin
   * ```
   *    public class usage.Usage extends groovyPlugin.GroovyClass {
   *       public void foo() {
   *          // virtual method resolution that requires loading of GroovyClass and subsequently, JavaClass.
   *          hashCode();
   *       }
   *    }
   * ```
   */
  @Test
  fun `plugin depends on other plugin that refers to Java classes, must not produce warning`() {
    val javaClass = ByteBuddy()
      .subclass(Any::class.java)
      .name("javaPlugin.JavaClass")
      .make()

    val groovyClass = ByteBuddy()
      .subclass(javaClass.typeDescription)
      .name("groovyPlugin.GroovyClass")
      .make()

    val usageClass = ByteBuddy()
      .subclass(groovyClass.typeDescription)
      .name("usage.Usage")
      .defineMethod("foo", Void.TYPE, Visibility.PUBLIC)
      .intercept(
        MethodCall.invoke(
          ElementMatchers.isHashCode()
        )
      )
      .make()

    val idePlugin = buildIdePlugin {
      dir("usage") {
        file("Usage.class", usageClass.bytes)
      }
    }

    val ide = buildIdeWithBundledPlugins(
      {
        dir("javaPlugin") {
          file("JavaClass.class", javaClass.bytes)
        }
      },
      {
        dir("groovyPlugin") {
          file("GroovyClass.class", groovyClass.bytes)
        }
      }
    )

    // Run verification
    val verificationResult = VerificationRunner().runPluginVerification(ide, idePlugin) as PluginVerificationResult.Verified

    // No warnings should be produced

    assertEquals(emptySet<CompatibilityProblem>(), verificationResult.compatibilityProblems)
    assertEquals(emptySet<CompatibilityWarning>(), verificationResult.compatibilityWarnings)
  }


  private fun buildIdePlugin(
    pluginClassesContentBuilder: (ContentBuilder).() -> Unit
  ): IdePlugin {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.jar")) {
      this.pluginClassesContentBuilder()

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
              <depends>org.intellij.groovy</depends>
            </idea-plugin>
            """.trimIndent()
        }
      }
    }

    return (IdePluginManager.createManager().createPlugin(pluginFile) as PluginCreationSuccess).plugin
  }

  private fun buildIdeWithBundledPlugins(
    javaPluginClassesBuilder: (ContentBuilder).() -> Unit,
    groovyPluginClassesBuilder: (ContentBuilder).() -> Unit
  ): Ide {
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

              //Generate content of Java plugin.
              this.javaPluginClassesBuilder()
            }
          }
        }
        dir("groovy") {
          dir("lib") {
            zip("groovy.jar") {
              dir("META-INF") {
                file("plugin.xml") {
                  """
                    <idea-plugin>
                      <id>org.intellij.groovy</id>
                      <depends>com.intellij.modules.java</depends>
                    </idea-plugin>
                  """
                }
              }

              //Generate content of Groovy plugin.
              this.groovyPluginClassesBuilder()
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

    val groovyPlugin = ide.bundledPlugins.find { it.pluginId == "org.intellij.groovy" }!!
    assertEquals("org.intellij.groovy", groovyPlugin.pluginId)

    return ide
  }
}