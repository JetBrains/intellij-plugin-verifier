package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.warnings.DynamicPluginStatus
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DynamicPluginStatusTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  companion object {

    private const val PLACE_HOLDER = "<PLACE_HOLDER/>"

    private val BASE_PLUGIN_XML = """
          <idea-plugin>
            <id>someId</id>
            <name>someName</name>
            <version>someVersion</version>
            ""<vendor email="vendor.com" url="url">vendor</vendor>""
            <description>this description is looooooooooong enough</description>
            <change-notes>these change-notes are looooooooooong enough</change-notes>
            <idea-version since-build="131.1"/>
            $PLACE_HOLDER
          </idea-plugin>
          """.trimIndent()
  }

  @Test
  fun `empty plugin can be safely loaded and unload immediately`() {
    checkPlugin(DynamicPluginStatus.AllowLoadUnloadImmediately, "")
  }

  @Test
  fun `plugin declaring only predefined extension points can be loaded and unloaded immediately`() {
    checkPlugin(
      DynamicPluginStatus.AllowLoadUnloadImmediately,
      """
        <extensions defaultExtensionNs="com.intellij">
           <themeProvider someParam="someValue"/>
           <bundledKeymap someParam="someValue"/>
           <bundledKeymapProvider someParam="someValue"/>
        </extensions>
      """.trimIndent()
    )
  }

  @Test
  fun `plugin declaring only its own extension points can be loaded and unloaded without restart`() {
    checkPlugin(
      DynamicPluginStatus.AllowLoadUnloadWithoutRestart(setOf(
        "Plugin declares extension points other than com.intellij.themeProvider, com.intellij.bundledKeymap, com.intellij.bundledKeymapProvider"
      )),
      """
        <extensionPoints>
          <extensionPoint name="ownEP" interface="doesntMatter"/>
        </extensionPoints>
        
        <extensions defaultExtensionNs="someId">
          <ownEP someKey="someValue"/>
        </extensions>
      """.trimIndent()
    )
  }

  @Test
  fun `plugin declaring only dynamic extension points can be loaded and unloaded without restart`() {
    checkPlugin(
      DynamicPluginStatus.AllowLoadUnloadWithoutRestart(setOf(
        "Plugin declares extension points other than com.intellij.themeProvider, com.intellij.bundledKeymap, com.intellij.bundledKeymapProvider"
      )),
      """
        <extensions defaultExtensionNs="com.intellij">
          <dynamicEP someKey="someValue"/>
        </extensions>
      """.trimIndent()
    )
  }

  @Test
  fun `plugin declaring non dynamic extension point is not dynamic`() {
    checkPlugin(
      DynamicPluginStatus.NotDynamic(
        setOf("Plugin declares extension points other than com.intellij.themeProvider, com.intellij.bundledKeymap, com.intellij.bundledKeymapProvider"),
        setOf("Plugin declares non-dynamic extensions: com.intellij.nonDynamicEP")
      ),
      """
        <extensions defaultExtensionNs="com.intellij">
          <nonDynamicEP someKey="someValue"/>
        </extensions>
      """.trimIndent()
    )
  }

  @Test
  fun `plugin declaring components is not dynamic`() {
    val reasons = setOf(
      "Plugin declares project components: SomeProjectComponent",
      "Plugin declares module components: SomeModuleComponent",
      "Plugin declares application components: SomeApplicationComponent"
    )
    checkPlugin(
      DynamicPluginStatus.NotDynamic(reasons, reasons),
      """
        <application-components>
            <component>
                <implementation-class>SomeApplicationComponent</implementation-class>
            </component>
        </application-components>

        <project-components>
            <component>
                <implementation-class>SomeProjectComponent</implementation-class>
            </component>
        </project-components>

        <module-components>
            <component>
                <implementation-class>SomeModuleComponent</implementation-class>
            </component>
        </module-components>
      """.trimIndent()
    )
  }

  @Test
  fun `plugin declaring actions cant be loaded immediately`() {
    checkPlugin(
      DynamicPluginStatus.AllowLoadUnloadWithoutRestart(setOf(
        "Plugin declares actions or groups, which can't be loaded immediately"
      )),
      """
        <actions>
            <action class="someClass"/>
        </actions>
      """.trimIndent()
    )
  }

  @Test
  fun `plugin declaring a group with no ID specified is not dynamic`() {
    checkPlugin(
      DynamicPluginStatus.NotDynamic(
        setOf("Plugin declares actions or groups, which can't be loaded immediately"),
        setOf("Plugin declares a group with no ID specified. Groups without ID can't be unloaded")
      ),
      """
        <actions>
            <group/>
        </actions>
      """.trimIndent()
    )
  }

  @Test
  fun `plugin declaring an action with no ID specified is not dynamic`() {
    checkPlugin(
      DynamicPluginStatus.NotDynamic(
        setOf("Plugin declares actions or groups, which can't be loaded immediately"),
        setOf("Plugin declares an action with neither 'id' nor 'class' specified")
      ),
      """
        <actions>
            <action>
            </action>
        </actions>
      """.trimIndent()
    )
  }

  private fun checkPlugin(dynamicStatus: DynamicPluginStatus, pluginXmlExtension: String) {
    val pluginXmlContent = BASE_PLUGIN_XML.replace(PLACE_HOLDER, pluginXmlExtension)
    val idePlugin = buildPlugin(pluginXmlContent)
    val ide = buildIde()
    val verificationResult = runVerification(ide, idePlugin)
    assertEquals(dynamicStatus, verificationResult.dynamicPluginStatus)
  }

  private fun runVerification(ide: Ide, idePlugin: IdePlugin) =
    VerificationRunner().runPluginVerification(ide, idePlugin) as PluginVerificationResult.Verified

  private fun buildPlugin(pluginXmlContent: String): IdePlugin {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.jar")) {
      dir("META-INF") {
        file("plugin.xml", pluginXmlContent)
      }
    }
    val pluginCreationResult = IdePluginManager.createManager().createPlugin(pluginFile)
    if (pluginCreationResult is PluginCreationFail) {
      Assert.fail(pluginCreationResult.errorsAndWarnings.joinToString { it.message })
    }
    return (pluginCreationResult as PluginCreationSuccess).plugin
  }

  private fun buildIde(): Ide {
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
    }

    val ide = IdeManager.createManager().createIde(ideaDirectory)
    assertEquals("IU-192.1", ide.version.asString())
    return ide
  }
}