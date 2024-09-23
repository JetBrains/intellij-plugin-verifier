package com.jetbrains.pluginverifier.tests

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.SimpleResourceResolver
import com.jetbrains.plugin.structure.intellij.xinclude.MetaInfResourceResolver
import com.jetbrains.plugin.structure.jar.META_INF
import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import com.jetbrains.pluginverifier.PluginVerificationResult
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

class WindowsCrossDriveVerificationTest : BasePluginTest() {
  private lateinit var fileSystem: FileSystem

  override val pluginJarPath: Path
    get() = fileSystem.getPath("""D:\plugin.jar""")

  @Before
  fun setUp() {
    val fsConfig = Configuration.windows()
      .toBuilder()
      .setRoots("""C:\""", """D:\""")
      .build()
    fileSystem = Jimfs.newFileSystem(fsConfig)
  }


  @Test
  fun test() {
    val ideRootPath = fileSystem.getPath("""C:\idea""")
    if (!ideRootPath.exists()) {
      Files.createDirectories(ideRootPath)
    }
    val ide = buildIde(ideRootPath = ideRootPath)

    val ideaPlugin = ideaPlugin()
    val creationResult = buildPluginWithResult {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $ideaPlugin
              <depends>org.jetbrains.plugins.emojipicker</depends>
            </idea-plugin>
          """
        }
      }
    }

    assertSuccess(creationResult) {
      val verificationResult = VerificationRunner().runPluginVerification(ide, plugin)
      assertTrue(verificationResult is PluginVerificationResult.Verified)
      val verifiedResult = verificationResult as PluginVerificationResult.Verified
      println(verifiedResult)
    }
  }

  @Test
  fun `simple resource resolver resolves a file`() {
    val resolver = SimpleResourceResolver
    val dataDir = dir("""C:\plugin""")
    dataDir.file(PLUGIN_XML)

    val result = resolver.resolveResource(PLUGIN_XML, dataDir)
    assertPathMatches("""C:\plugin\$PLUGIN_XML""", result)
  }

  @Test
  fun `resource is resolved in META-INF`() {
    val resolver = MetaInfResourceResolver(SimpleResourceResolver)
    val componentsDir = dir("""C:\plugin\components""")
    val metaInfDir = dir("""C:\plugin\$META_INF""")
    val pluginXml = metaInfDir.file(PLUGIN_XML)

    val result = resolver.resolveResource(PLUGIN_XML, componentsDir)
    assertFound(pluginXml, result)
  }

  @Test
  fun `resource is resolved in META-INF on the drive D`() {
    val resolver = MetaInfResourceResolver(SimpleResourceResolver)
    val componentsDir = dir("""D:\plugin\components""")
    val metaInfDir = dir("""D:\plugin\META-INF""")
    val pluginXml = metaInfDir.file(PLUGIN_XML)

    val result = resolver.resolveResource(PLUGIN_XML, componentsDir)
    assertFound(pluginXml, result)
  }

  @Test
  fun `resource is resolved in META-INF in the root of the drive D`() {
    val resolver = MetaInfResourceResolver(SimpleResourceResolver)
    val componentsDir = dir("""D:\components""")
    val metaInfDir = dir("""D:\META-INF""")
    val pluginXml = metaInfDir.resolve(PLUGIN_XML).createFile()

    val result = resolver.resolveResource(PLUGIN_XML, componentsDir)
    assertFound(pluginXml, result)
  }

  @Test
  fun `resource is not resolved in META-INF as there is no parent dir in the filesystem tree`() {
    val resolver = MetaInfResourceResolver(SimpleResourceResolver)
    val dir = dir("""D:\""")
    val metaInfDir = dir("""D:\META-INF""")
    val pluginXml = metaInfDir.resolve(PLUGIN_XML).createFile()

    val result = resolver.resolveResource(PLUGIN_XML, dir)
    assertFound(pluginXml, result)
  }

  private fun assertPathMatches(expectedPath: String, result: ResourceResolver.Result) {
    assertTrue("Expected 'Found' result, but got ${result.javaClass}", result is ResourceResolver.Result.Found)
    with(result as ResourceResolver.Result.Found) {
      assertEquals(expectedPath, path.toString())
    }
  }

  private fun assertFound(expectedPath: Path, result: ResourceResolver.Result) {
    assertTrue("Expected 'Found' result, but got ${result.javaClass}", result is ResourceResolver.Result.Found)
    with(result as ResourceResolver.Result.Found) {
      assertEquals(expectedPath, path)
    }
  }

  private fun dir(path: String): Path {
    return fileSystem.getPath(path).createDirectories()
  }

  private fun Path.file(path: String): Path {
    return this.resolve(path).createFile()
  }

  private fun buildIde(version: String = "IU-192.1", ideRootPath: Path): Ide {
    val ideaDirectory = buildDirectory(ideRootPath) {
      file("build.txt", version)
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
        dir("emojipicker") {
          dir("lib") {
            zip("emojipicker.jar") {
              dir("META-INF") {
                file("plugin.xml") {
                  """
                  <idea-plugin implementation-detail="true">
                    <name>Emoji Picker</name>
                    <id>org.jetbrains.plugins.emojipicker</id>
                    <version>242.8256</version>
                    <idea-version since-build="242.8256" until-build="242.8256"/>
                    <vendor>JetBrains</vendor>
                    <description>
                      <![CDATA[
                        Popup window, allowing to select and insert emoji into editor & text fields.
                        Can be opened with <b>Edit > Emoji & Symbols</b> as well as through context
                        menu or keyboard shortcut (<b>Ctrl + Alt + ;</b> by default)
                      ]]>
                    </description>
                    <depends>com.intellij.modules.platform</depends>
                  </idea-plugin>                    
                  """.trimIndent()
                }
              }
            }
          }
        }
      }
    }
    val ide = IdeManager.createManager().createIde(ideaDirectory)
    assertEquals(version, ide.version.asString())
    return ide
  }

  @After
  fun tearDown() {
    fileSystem.close()
  }
}