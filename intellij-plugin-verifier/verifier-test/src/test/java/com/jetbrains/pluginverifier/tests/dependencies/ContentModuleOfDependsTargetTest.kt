/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests.dependencies

import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginArchiveManager
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.resolution.DefaultClassResolverProvider
import com.jetbrains.pluginverifier.tests.BasePluginTest
import com.jetbrains.pluginverifier.tests.mocks.MockDependencyFinder
import com.jetbrains.pluginverifier.tests.mocks.MockPackageFilter
import com.jetbrains.pluginverifier.tests.mocks.buildIdePlugin
import com.jetbrains.pluginverifier.tests.mocks.createPluginArchiveManager
import com.jetbrains.pluginverifier.tests.mocks.getDetails
import org.intellij.lang.annotations.Language
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ACC_SUPER
import org.objectweb.asm.Opcodes.V1_8

private const val IDE_VERSION = "PS-263.4589"

private const val PHP_PLUGIN_ID = "com.jetbrains.php"
private const val PHP_MAIN_MODULE_CLASS = "com/jetbrains/php/lang/PhpLanguage"

private const val FRONTBACK_IMPL_MODULE = "intellij.php.frontback.impl"

/**
 * A bundled plugin that depends on the PHP plugin, such as Laravel Idea in PhpStorm.
 */
private const val BUNDLED_PLUGIN_ID = "com.example.bundledPhpExtension"

private class ContentModule(val name: String, val loading: String?, val className: String)

private val PHP_CONTENT_MODULES = listOf(
  ContentModule(FRONTBACK_IMPL_MODULE, "embedded", "com/jetbrains/php/frontback/PhpFrontbackApi"),
  ContentModule("intellij.php.backend", loading = null, "com/jetbrains/php/PhpIndexImpl"),
  ContentModule("intellij.php.debugger", "required", "com/jetbrains/php/debug/PhpDebugDriver"),
  ContentModule("intellij.php.remote.interpreter", "on-demand", "com/jetbrains/php/remote/PhpRemoteInterpreter"),
)

private class LayoutComponent(val name: String, val kind: String, val classPath: List<String>)

/**
 * A plugin that declares a `<depends>com.jetbrains.php</depends>` must see the classes of all
 * content modules of the PHP plugin, such as `intellij.php.backend`, whatever their loading rule.
 *
 * By default, the IDE built here mirrors the PhpStorm layout: the `intellij.php.frontback.impl` content module is
 * bundled in the plugin `lib` directory, while `product-info.json` points to a non-existent JAR in
 * `lib/modules`, so this layout component is skipped and cannot be resolved on its own.
 */
class ContentModuleOfDependsTargetTest : BasePluginTest() {

  private lateinit var archiveManager: PluginArchiveManager

  @Before
  fun setUp() {
    archiveManager = temporaryFolder.createPluginArchiveManager()
  }

  @After
  fun tearDown() {
    archiveManager.close()
  }

  @Test
  fun `every plugin depending on the PHP plugin sees its content module classes, not just the first one`() {
    withResolverProvider(buildPhpStormLikeIde()) { resolverProvider ->
      repeat(3) { i ->
        val plugin = buildPlugin("com.example.phpExtension$i", "<depends>$PHP_PLUGIN_ID</depends>")
        resolverProvider.assertResolvesPhpClasses(plugin, "Plugin #$i")
      }
    }
  }

  @Test
  fun `plugin with an optional dependency on the PHP plugin sees its content module classes`() {
    withResolverProvider(buildPhpStormLikeIde()) { resolverProvider ->
      val plugin = buildPlugin(
        "com.example.optionalPhpExtension",
        """<depends optional="true" config-file="php-support.xml">$PHP_PLUGIN_ID</depends>""",
        optionalConfigFile = "php-support.xml"
      )
      resolverProvider.assertResolvesPhpClasses(plugin, "The plugin")
    }
  }

  @Test
  fun `plugin depending on the PHP plugin sees its content module classes when every module has its own JAR`() {
    withResolverProvider(buildPhpStormLikeIde(frontbackImplJarInModulesDir = true)) { resolverProvider ->
      val plugin = buildPlugin("com.example.phpExtension", "<depends>$PHP_PLUGIN_ID</depends>")
      resolverProvider.assertResolvesPhpClasses(plugin, "The plugin")
    }
  }

  @Test
  fun `plugin depending on a plugin that depends on the PHP plugin sees its content module classes`() {
    withResolverProvider(buildPhpStormLikeIde()) { resolverProvider ->
      val plugin = buildPlugin("com.example.transitivePhpExtension", "<depends>$BUNDLED_PLUGIN_ID</depends>")
      resolverProvider.assertResolvesPhpClasses(plugin, "The plugin")
    }
  }

  /**
   * Runs the [block] against a resolver provider that has already verified another plugin depending on the PHP plugin,
   * as in `check-ide`, where all plugins verified against an IDE share the resolver caches.
   */
  private fun withResolverProvider(ide: Ide, block: (DefaultClassResolverProvider) -> Unit) {
    IdeDescriptor.create(ide.idePath, defaultJdkPath = null, ideFileLock = null).use { ideDescriptor ->
      val resolverProvider = DefaultClassResolverProvider(MockDependencyFinder(), ideDescriptor, MockPackageFilter(), archiveManager = archiveManager)
      val firstPlugin = buildPlugin("com.example.firstPhpExtension", "<depends>$PHP_PLUGIN_ID</depends>")
      resolverProvider.assertResolvesPhpClasses(firstPlugin, "The first verified plugin")
      block(resolverProvider)
    }
  }

  private fun DefaultClassResolverProvider.assertResolvesPhpClasses(plugin: IdePlugin, subject: String) {
    with(provide(plugin.getDetails()).allResolver) {
      assertTrue("$subject must resolve the classes of the PHP plugin main module", containsClass(PHP_MAIN_MODULE_CLASS))
      PHP_CONTENT_MODULES.forEach {
        assertTrue(
          "$subject must resolve the classes of the '${it.name}' content module of the PHP plugin",
          containsClass(it.className)
        )
      }
    }
  }

  private fun buildPlugin(pluginId: String, dependsTag: String, optionalConfigFile: String? = null): IdePlugin {
    return temporaryFolder.newFile("$pluginId.jar").toPath().buildIdePlugin {
      dir("META-INF") {
        file("plugin.xml", "<idea-plugin>" + ideaPlugin(pluginId = pluginId, sinceBuild = "263.1", untilBuild = "263.9999") + dependsTag + "</idea-plugin>")
        optionalConfigFile?.let { file(it, "<idea-plugin/>") }
      }
    }
  }

  /**
   * @param frontbackImplJarInModulesDir whether the `intellij.php.frontback.impl` JAR is in `lib/modules`,
   * where its layout component points to, rather than in the PHP plugin `lib` directory
   */
  private fun buildPhpStormLikeIde(frontbackImplJarInModulesDir: Boolean = false): Ide {
    val phpPluginClassPath = listOf("plugins/php-impl/lib/php.jar") +
      if (frontbackImplJarInModulesDir) emptyList() else listOf("plugins/php-impl/lib/$FRONTBACK_IMPL_MODULE.jar")
    val layout = listOf(
      LayoutComponent("com.intellij", "plugin", listOf("lib/app.jar")),
      LayoutComponent(PHP_PLUGIN_ID, "plugin", phpPluginClassPath),
      LayoutComponent(BUNDLED_PLUGIN_ID, "plugin", listOf("plugins/bundled-php-extension/lib/bundled-php-extension.jar")),
    ) + PHP_CONTENT_MODULES.map {
      LayoutComponent(it.name, "moduleV2", listOf("plugins/php-impl/lib/modules/${it.name}.jar"))
    }

    val ideRoot = buildDirectory(ideaPath) {
      file("build.txt", IDE_VERSION)
      file("product-info.json", productInfoJson(layout))
      dir("lib") {
        zip("app.jar") {
          dir("META-INF") {
            file("plugin.xml", corePluginXml)
          }
          classFile("com/intellij/openapi/PlatformApi")
        }
      }
      dir("modules") {
        zip("module-descriptors.jar") { /* content modules are declared inline in 'plugin.xml' */ }
      }
      dir("plugins") {
        dir("bundled-php-extension") {
          dir("lib") {
            zip("bundled-php-extension.jar") {
              dir("META-INF") {
                file("plugin.xml", bundledPluginXml)
              }
            }
          }
        }
        dir("php-impl") {
          dir("lib") {
            zip("php.jar") {
              dir("META-INF") {
                file("plugin.xml", phpPluginXml)
              }
              classFile(PHP_MAIN_MODULE_CLASS)
            }
            val (modulesDirModules, libDirModules) = PHP_CONTENT_MODULES.partition {
              frontbackImplJarInModulesDir || it.name != FRONTBACK_IMPL_MODULE
            }
            libDirModules.forEach { moduleJar(it) }
            dir("modules") {
              modulesDirModules.forEach { moduleJar(it) }
            }
          }
        }
      }
    }
    return IdeManager.createManager().createIde(ideRoot)
  }

  private fun ContentBuilder.moduleJar(module: ContentModule) {
    zip("${module.name}.jar") {
      classFile(module.className)
    }
  }

  private fun ContentBuilder.classFile(binaryName: String) {
    dirs(binaryName.substringBeforeLast('/')) {
      file(binaryName.substringAfterLast('/') + ".class", emptyClass(binaryName))
    }
  }

  @Language("XML")
  private val corePluginXml = """
    <idea-plugin>
      <id>com.intellij</id>
      <name>PhpStorm Core</name>
      <version>1.0</version>
      <module value="com.intellij.modules.platform"/>
      <module value="com.intellij.modules.lang"/>
      <module value="com.intellij.modules.all"/>
    </idea-plugin>
  """.trimIndent()

  private val phpPluginXml = """
    <idea-plugin package="com.jetbrains.php">
      <id>$PHP_PLUGIN_ID</id>
      <name>PHP</name>
      <vendor>JetBrains</vendor>
      <dependencies>
        <module name="com.intellij.modules.platform"/>
      </dependencies>
      <content>
${PHP_CONTENT_MODULES.joinToString("\n") { it.toContentXml() }}
      </content>
    </idea-plugin>
  """.trimIndent()

  private fun ContentModule.toContentXml(): String {
    val loadingAttribute = loading?.let { " loading=\"$it\"" } ?: ""
    return """
        <module name="$name"$loadingAttribute><![CDATA[<idea-plugin visibility="public">
          <dependencies>
            <module name="com.intellij.modules.platform"/>
          </dependencies>
        </idea-plugin>]]></module>
    """.trimEnd()
  }

  @Language("XML")
  private val bundledPluginXml = """
    <idea-plugin>
      <id>$BUNDLED_PLUGIN_ID</id>
      <name>Bundled PHP Extension</name>
      <version>1.0</version>
      <depends>com.intellij.modules.platform</depends>
      <depends>$PHP_PLUGIN_ID</depends>
    </idea-plugin>
  """.trimIndent()

  private fun productInfoJson(layout: List<LayoutComponent>): String {
    val bundledPlugins = layout.filter { it.kind == "plugin" && it.name != "com.intellij" }
    return """
      {
        "name": "PhpStorm",
        "version": "2026.3",
        "buildNumber": "263.4589",
        "productCode": "PS",
        "envVarBaseName": "PHPSTORM",
        "dataDirectoryName": "PhpStorm2026.3",
        "svgIconPath": "../bin/phpstorm.svg",
        "productVendor": "JetBrains",
        "launch": [
          {
            "os": "Linux",
            "arch": "amd64",
            "launcherPath": "bin/phpstorm.sh",
            "javaExecutablePath": "jbr/bin/java",
            "vmOptionsFilePath": "bin/phpstorm64.vmoptions",
            "bootClassPathJarNames": ["app.jar"]
          }
        ],
        "bundledPlugins": [${bundledPlugins.joinToString { "\"${it.name}\"" }}],
        "modules": [],
        "layout": [${layout.joinToString(",") { it.toJson() }}]
      }
    """.trimIndent()
  }

  private fun LayoutComponent.toJson(): String =
    """{"name": "$name", "kind": "$kind", "classPath": [${classPath.joinToString { "\"$it\"" }}]}"""

  private fun emptyClass(binaryName: String): ByteArray = ClassWriter(0).apply {
    visit(V1_8, ACC_PUBLIC or ACC_SUPER, binaryName, null, "java/lang/Object", null)
    visitEnd()
  }.toByteArray()
}
