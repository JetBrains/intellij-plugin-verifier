package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginProviderResult
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvision
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvision.Source
import com.jetbrains.plugin.structure.intellij.plugin.PluginQuery
import com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path
import java.nio.file.Paths

private const val CORE_PLUGIN_ID = "com.intellij"
private const val CORE_PLUGIN_ALIAS = "com.intellij.modules.platform"
private const val CORE_PLUGIN_NAME = "IDEA CORE"
private const val PRODUCT_MODULE_NAME = "intellij.platform.diagnostic.freezeAnalyzer"
private const val PRODUCT_MODULE_JAR = "lib/modules/$PRODUCT_MODULE_NAME.jar"
private const val PLUGIN_MODULE_NAME = "intellij.platform.kernel.backend.split"
private const val PLUGIN_MODULE_JAR = "plugins/cwm-plugin/lib/modules/$PLUGIN_MODULE_NAME.jar"
private const val LAYOUT_PLUGIN_ID = "intellij.platform.ijent.impl"
private const val LAYOUT_PLUGIN_JAR = "plugins/platform-ijent-impl/lib/platform-ijent-impl.jar"

class IdeFindPluginByIdOrModuleIdTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var ideRoot: Path

  private lateinit var ide: Ide

  @Before
  fun setUp() {
    ideRoot = buildDirectory(temporaryFolder.newFolder("idea").toPath()) {
      file("build.txt", "IU-243.21565.193")
      file("product-info.json", productInfoJson)
      dir("lib") {
        zip("product.jar") {
          dir("META-INF") {
            file("plugin.xml", corePluginXml)
          }
        }
        dir("modules") {
          zip("$PRODUCT_MODULE_NAME.jar") {
            file("$PRODUCT_MODULE_NAME.xml") {
              """
                <idea-plugin package="com.intellij.diagnostic.freezeAnalyzer">
                </idea-plugin>
              """.trimIndent()
            }
          }
        }
      }
      dir("plugins") {
        dir("platform-ijent-impl") {
          dir("lib") {
            zip("platform-ijent-impl.jar") {
              dir("META-INF") {
                file("plugin.xml", layoutPluginXml)
              }
            }
          }
        }
        dir("cwm-plugin") {
          dir("lib") {
            dir("modules") {
              zip("$PLUGIN_MODULE_NAME.jar") {
                file("$PLUGIN_MODULE_NAME.xml") {
                  """
                    <idea-plugin package="com.intellij.platform.kernel.backend.split">
                    </idea-plugin>
                  """.trimIndent()
                }
              }
            }
          }
        }
      }
      dir("modules") {
        zip("module-descriptors.jar") {
          file("$PRODUCT_MODULE_NAME.xml") {
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <module name="$PRODUCT_MODULE_NAME">
                <resources>
                  <resource-root path="../$PRODUCT_MODULE_JAR"/>
                </resources>
              </module>
            """.trimIndent()
          }
          file("$PLUGIN_MODULE_NAME.xml") {
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <module name="$PLUGIN_MODULE_NAME">
                <resources>
                  <resource-root path="../$PLUGIN_MODULE_JAR"/>
                </resources>
              </module>
            """.trimIndent()
          }
        }
      }
    }
    ide = ProductInfoBasedIdeManager().createIde(ideRoot)
  }

  @Test
  fun `getBundledPlugins returns core plugin, layout plugin and IDE modules`() {
    val bundledPlugins = ide.bundledPlugins
    assertEquals(4, bundledPlugins.size)
    assertEquals(
      listOf(CORE_PLUGIN_ID, LAYOUT_PLUGIN_ID, PLUGIN_MODULE_NAME, PRODUCT_MODULE_NAME).sorted(),
      bundledPlugins.mapNotNull { it.pluginId }.sorted()
    )

    val ideModuleIds = bundledPlugins.filterIsInstance<IdeModule>().mapNotNull { it.pluginId }
    assertEquals(listOf(PLUGIN_MODULE_NAME, PRODUCT_MODULE_NAME).sorted(), ideModuleIds.sorted())

    val corePlugin = bundledPlugins.single { it.pluginId == CORE_PLUGIN_ID }
    assertFalse(corePlugin is IdeModule)
    assertTrue(CORE_PLUGIN_ALIAS in corePlugin.definedModules)

    val layoutPlugin = bundledPlugins.single { it.pluginId == LAYOUT_PLUGIN_ID }
    assertFalse(layoutPlugin is IdeModule)
  }

  @Test
  fun `hasBundledPlugin matches layout plugin and IDE module names`() {
    assertTrue(ide.hasBundledPlugin(LAYOUT_PLUGIN_ID))
    assertTrue(ide.hasBundledPlugin(PRODUCT_MODULE_NAME))
    assertTrue(ide.hasBundledPlugin(PLUGIN_MODULE_NAME))
  }

  @Test
  fun `hasBundledPlugin does not match plugin alias or unknown ID`() {
    assertFalse(ide.hasBundledPlugin(CORE_PLUGIN_ALIAS))
    assertFalse(ide.hasBundledPlugin("unknown.plugin.id"))
  }

  @Test
  fun `findPluginByModule resolves plugin alias to core plugin`() {
    val plugin = ide.findPluginByModule(CORE_PLUGIN_ALIAS)
    assertNotNull(plugin)
    plugin!!
    assertEquals(CORE_PLUGIN_ID, plugin.pluginId)
    assertFalse(plugin is IdeModule)
  }

  @Test
  fun `findPluginByModule matches IDE module names`() {
    listOf(PRODUCT_MODULE_NAME, PLUGIN_MODULE_NAME).forEach { moduleName ->
      val plugin = ide.findPluginByModule(moduleName)
      assertNotNull("Module '$moduleName' must be found by its name", plugin)
      plugin!!
      assertTrue("Expected an IdeModule, but got ${plugin.javaClass.name}", plugin is IdeModule)
      assertEquals(moduleName, plugin.pluginId)
    }
  }

  @Test
  fun `findPluginByModule does not match plugin IDs of regular plugins or unknown ID`() {
    assertNull(ide.findPluginByModule(CORE_PLUGIN_ID))
    assertNull(ide.findPluginByModule(LAYOUT_PLUGIN_ID))
    assertNull(ide.findPluginByModule("unknown.module.id"))
  }

  @Test
  fun `findPluginByIdOrModuleId returns product module as IdeModule`() {
    assertTrue(
      "Product module '$PRODUCT_MODULE_NAME' must be loaded among bundled plugins",
      ide.bundledPlugins.any { it is IdeModule && it.pluginId == PRODUCT_MODULE_NAME }
    )

    val result = ide.findPluginByIdOrModuleId(PRODUCT_MODULE_NAME)
    assertNotNull(result)
    result!!
    assertEquals(PluginProviderResult.Type.PLUGIN, result.type)

    val plugin = result.plugin
    assertTrue("Expected an IdeModule, but got ${plugin.javaClass.name}", plugin is IdeModule)
    plugin as IdeModule
    assertEquals(PRODUCT_MODULE_NAME, plugin.pluginId)
    assertTrue(
      "Classpath ${plugin.classpath.paths} must contain '$PRODUCT_MODULE_JAR'",
      plugin.classpath.paths.any { it.endsWith(Paths.get(PRODUCT_MODULE_JAR)) }
    )
  }

  @Test
  fun `findPluginByIdOrModuleId returns plugin content module as IdeModule`() {
    assertTrue(
      "Module '$PLUGIN_MODULE_NAME' must be loaded among bundled plugins",
      ide.bundledPlugins.any { it is IdeModule && it.pluginId == PLUGIN_MODULE_NAME }
    )

    val result = ide.findPluginByIdOrModuleId(PLUGIN_MODULE_NAME)
    assertNotNull(result)
    result!!
    assertEquals(PluginProviderResult.Type.PLUGIN, result.type)

    val plugin = result.plugin
    assertTrue("Expected an IdeModule, but got ${plugin.javaClass.name}", plugin is IdeModule)
    plugin as IdeModule
    assertEquals(PLUGIN_MODULE_NAME, plugin.pluginId)
    assertTrue(
      "Classpath ${plugin.classpath.paths} must contain '$PLUGIN_MODULE_JAR'",
      plugin.classpath.paths.any { it.endsWith(Paths.get(PLUGIN_MODULE_JAR)) }
    )
  }

  @Test
  fun `findPluginByIdOrModuleId returns core plugin as PLUGIN`() {
    val result = ide.findPluginByIdOrModuleId(CORE_PLUGIN_ID)
    assertNotNull(result)
    result!!
    assertEquals(PluginProviderResult.Type.PLUGIN, result.type)
    assertEquals(CORE_PLUGIN_ID, result.plugin.pluginId)
    assertFalse(result.plugin is IdeModule)
  }

  @Test
  fun `findPluginByIdOrModuleId returns layout plugin as PLUGIN`() {
    val result = ide.findPluginByIdOrModuleId(LAYOUT_PLUGIN_ID)
    assertNotNull(result)
    result!!
    assertEquals(PluginProviderResult.Type.PLUGIN, result.type)

    val plugin = result.plugin
    assertEquals(LAYOUT_PLUGIN_ID, plugin.pluginId)
    assertFalse("Expected a regular plugin, but got an IdeModule", plugin is IdeModule)
    assertTrue(
      "Classpath ${plugin.classpath.paths} must contain '$LAYOUT_PLUGIN_JAR'",
      plugin.classpath.paths.any { it.endsWith(Paths.get(LAYOUT_PLUGIN_JAR)) }
    )
  }

  @Test
  fun `findPluginByIdOrModuleId resolves plugin alias as MODULE`() {
    val result = ide.findPluginByIdOrModuleId(CORE_PLUGIN_ALIAS)
    assertNotNull(result)
    result!!
    assertEquals(PluginProviderResult.Type.MODULE, result.type)
    assertEquals(CORE_PLUGIN_ID, result.plugin.pluginId)
  }

  @Test
  fun `query by ID finds regular plugins`() {
    listOf(CORE_PLUGIN_ID, LAYOUT_PLUGIN_ID).forEach { pluginId ->
      val plugin = assertFound(PluginQuery.Builder.of(pluginId).inId(), Source.ID)
      assertEquals(pluginId, plugin.pluginId)
      assertFalse("Expected a regular plugin, but got an IdeModule", plugin is IdeModule)
    }
  }

  @Test
  fun `query by ID finds IDE modules`() {
    listOf(PRODUCT_MODULE_NAME, PLUGIN_MODULE_NAME).forEach { moduleName ->
      val plugin = assertFound(PluginQuery.Builder.of(moduleName).inId(), Source.ID)
      assertTrue("Expected an IdeModule, but got ${plugin.javaClass.name}", plugin is IdeModule)
      assertEquals(moduleName, plugin.pluginId)
    }
  }

  @Test
  fun `query by name finds core plugin`() {
    val plugin = assertFound(PluginQuery.Builder.of(CORE_PLUGIN_NAME).inName(), Source.NAME)
    assertEquals(CORE_PLUGIN_ID, plugin.pluginId)
  }

  @Test
  fun `query by name does not find IDE modules`() {
    listOf(PRODUCT_MODULE_NAME, PLUGIN_MODULE_NAME).forEach { moduleName ->
      assertNotFound(PluginQuery.Builder.of(moduleName).inName())
    }
  }

  @Test
  fun `query by plugin alias finds core plugin`() {
    val plugin = assertFound(PluginQuery.Builder.of(CORE_PLUGIN_ALIAS).inPluginAliases(), Source.ALIAS)
    assertEquals(CORE_PLUGIN_ID, plugin.pluginId)
    assertFalse(plugin is IdeModule)
  }

  @Test
  fun `query by plugin alias finds IDE modules by their names`() {
    listOf(PRODUCT_MODULE_NAME, PLUGIN_MODULE_NAME).forEach { moduleName ->
      val plugin = assertFound(PluginQuery.Builder.of(moduleName).inPluginAliases(), Source.ALIAS)
      assertTrue("Expected an IdeModule, but got ${plugin.javaClass.name}", plugin is IdeModule)
      assertEquals(moduleName, plugin.pluginId)
    }
  }

  @Test
  fun `query by content module ID does not find IDE modules by their own names`() {
    listOf(PRODUCT_MODULE_NAME, PLUGIN_MODULE_NAME).forEach { moduleName ->
      assertNotFound(PluginQuery.Builder.of(moduleName).inContentModuleId())
    }
  }

  @Test
  fun `query reports the first matching criterion of a plugin`() {
    val query = PluginQuery.Builder.of(PRODUCT_MODULE_NAME)
      .inId()
      .inName()
      .inPluginAliases()
      .inContentModuleId()
    val plugin = assertFound(query, Source.ID)
    assertTrue(plugin is IdeModule)
    assertEquals(PRODUCT_MODULE_NAME, plugin.pluginId)
  }

  @Test
  fun `query does not find unknown ID or match without enabled criteria`() {
    val unknownId = "unknown.plugin.id"
    assertNotFound(PluginQuery.Builder.of(unknownId).inId().inName().inPluginAliases().inContentModuleId())
    assertNotFound(PluginQuery.Builder.of(CORE_PLUGIN_ID))
  }

  private fun assertFound(query: PluginQuery.Builder, expectedSource: Source): IdePlugin {
    val provision = ide.query(query.build())
    assertTrue("Expected a plugin to be found for <${query.build()}>, but got $provision", provision is PluginProvision.Found)
    provision as PluginProvision.Found
    assertEquals(expectedSource, provision.source)
    return provision.plugin
  }

  private fun assertNotFound(query: PluginQuery.Builder) {
    val provision = ide.query(query.build())
    assertSame("Expected no plugin to be found for <${query.build()}>", PluginProvision.NotFound, provision)
  }

  private val productInfoJson = """
    {
      "name": "IntelliJ IDEA",
      "version": "2024.3",
      "buildNumber": "243.21565.193",
      "productCode": "IU",
      "dataDirectoryName": "IntelliJIdea2024.3",
      "svgIconPath": "bin/idea.svg",
      "productVendor": "JetBrains",
      "launch": [],
      "bundledPlugins": [],
      "modules": [],
      "fileExtensions": [],
      "layout": [
        {
          "name": "$PRODUCT_MODULE_NAME",
          "kind": "productModuleV2",
          "classPath": [
            "$PRODUCT_MODULE_JAR"
          ]
        },
        {
          "name": "$PLUGIN_MODULE_NAME",
          "kind": "moduleV2",
          "classPath": [
            "$PLUGIN_MODULE_JAR"
          ]
        },
        {
          "name": "$LAYOUT_PLUGIN_ID",
          "kind": "plugin",
          "classPath": [
            "$LAYOUT_PLUGIN_JAR"
          ]
        }
      ]
    }
  """.trimIndent()

  private val corePluginXml = """
    <idea-plugin>
      <id>$CORE_PLUGIN_ID</id>
      <name>$CORE_PLUGIN_NAME</name>
      <module value="$CORE_PLUGIN_ALIAS"/>
    </idea-plugin>
  """.trimIndent()

  private val layoutPluginXml = """
    <idea-plugin>
      <id>$LAYOUT_PLUGIN_ID</id>
      <name>IJent</name>
      <vendor>JetBrains</vendor>
      <version>243.21565.193</version>
      <idea-version since-build="243.21565.193" until-build="243.21565.193" />
    </idea-plugin>
  """.trimIndent()
}
