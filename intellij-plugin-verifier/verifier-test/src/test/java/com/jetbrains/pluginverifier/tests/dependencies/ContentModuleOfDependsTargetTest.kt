/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests.dependencies

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.intellij.plugin.PluginArchiveManager
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.resolution.DefaultClassResolverProvider
import com.jetbrains.pluginverifier.tests.BasePluginTest
import com.jetbrains.pluginverifier.tests.mocks.MockDependencyFinder
import com.jetbrains.pluginverifier.tests.mocks.MockPackageFilter
import com.jetbrains.pluginverifier.tests.mocks.buildIdePlugin
import com.jetbrains.pluginverifier.tests.mocks.createPluginArchiveManager
import com.jetbrains.pluginverifier.tests.mocks.descriptor
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

private const val PHP_MAIN_MODULE_CLASS = "com/jetbrains/php/lang/PhpLanguage"
private const val PHP_BACKEND_MODULE_CLASS = "com/jetbrains/php/PhpIndexImpl"

/**
 * A plugin that declares a plain `<depends>com.jetbrains.php</depends>` must see the classes of all
 * content modules of the PHP plugin, such as `intellij.php.backend`.
 *
 * The IDE built here mirrors the PhpStorm layout: the `intellij.php.frontback.impl` content module is
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
    val ide = buildPhpStormLikeIde()

    IdeDescriptor.create(ide.idePath, defaultJdkPath = null, ideFileLock = null).use { ideDescriptor ->
      // A single resolver provider is shared by all plugins verified against an IDE, as in `check-ide`.
      val resolverProvider = DefaultClassResolverProvider(
        MockDependencyFinder(),
        ideDescriptor,
        MockPackageFilter(),
        archiveManager = archiveManager
      )

      repeat(3) { i ->
        val plugin = temporaryFolder.newFile("php-extension-$i.jar").toPath().buildIdePlugin {
          descriptor(
            ideaPlugin(pluginId = "com.example.phpExtension$i", sinceBuild = "263.1", untilBuild = "263.9999") +
              "<depends>com.jetbrains.php</depends>"
          )
        }

        with(resolverProvider.provide(plugin.getDetails()).allResolver) {
          assertTrue(
            "Plugin #$i must resolve the classes of the PHP plugin main module",
            containsClass(PHP_MAIN_MODULE_CLASS)
          )
          assertTrue(
            "Plugin #$i must resolve the classes of the 'intellij.php.backend' content module of the PHP plugin",
            containsClass(PHP_BACKEND_MODULE_CLASS)
          )
        }
      }
    }
  }

  private fun buildPhpStormLikeIde(): Ide {
    val ideRoot = buildDirectory(ideaPath) {
      file("build.txt", IDE_VERSION)
      file("product-info.json", productInfoJson)
      dir("lib") {
        zip("app.jar") {
          dir("META-INF") {
            file("plugin.xml", corePluginXml)
          }
          dirs("com/intellij/openapi") {
            file("PlatformApi.class", emptyClass("com/intellij/openapi/PlatformApi"))
          }
        }
      }
      dir("modules") {
        zip("module-descriptors.jar") { /* content modules are declared inline in 'plugin.xml' */ }
      }
      dir("plugins") {
        dir("php-impl") {
          dir("lib") {
            zip("php.jar") {
              dir("META-INF") {
                file("plugin.xml", phpPluginXml)
              }
              dirs("com/jetbrains/php/lang") {
                file("PhpLanguage.class", emptyClass(PHP_MAIN_MODULE_CLASS))
              }
            }
            zip("intellij.php.frontback.impl.jar") {
              dirs("com/jetbrains/php/frontback") {
                file("PhpFrontbackApi.class", emptyClass("com/jetbrains/php/frontback/PhpFrontbackApi"))
              }
            }
            dir("modules") {
              zip("intellij.php.backend.jar") {
                dirs("com/jetbrains/php") {
                  file("PhpIndexImpl.class", emptyClass(PHP_BACKEND_MODULE_CLASS))
                }
              }
            }
          }
        }
      }
    }
    return IdeManager.createManager().createIde(ideRoot)
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

  @Language("XML")
  private val phpPluginXml = """
    <idea-plugin package="com.jetbrains.php">
      <id>com.jetbrains.php</id>
      <name>PHP</name>
      <vendor>JetBrains</vendor>
      <dependencies>
        <module name="com.intellij.modules.platform"/>
      </dependencies>
      <content>
        <module name="intellij.php.frontback.impl" loading="embedded"><![CDATA[<idea-plugin visibility="internal">
          <dependencies>
            <module name="com.intellij.modules.platform"/>
          </dependencies>
        </idea-plugin>]]></module>
        <module name="intellij.php.backend"><![CDATA[<idea-plugin visibility="public">
          <dependencies>
            <module name="com.intellij.modules.platform"/>
          </dependencies>
        </idea-plugin>]]></module>
      </content>
    </idea-plugin>
  """.trimIndent()

  @Language("JSON")
  private val productInfoJson = """
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
      "bundledPlugins": ["com.jetbrains.php"],
      "modules": [],
      "layout": [
        {
          "name": "com.intellij",
          "kind": "plugin",
          "classPath": ["lib/app.jar"]
        },
        {
          "name": "com.jetbrains.php",
          "kind": "plugin",
          "classPath": ["plugins/php-impl/lib/php.jar", "plugins/php-impl/lib/intellij.php.frontback.impl.jar"]
        },
        {
          "name": "intellij.php.frontback.impl",
          "kind": "moduleV2",
          "classPath": ["plugins/php-impl/lib/modules/intellij.php.frontback.impl.jar"]
        },
        {
          "name": "intellij.php.backend",
          "kind": "moduleV2",
          "classPath": ["plugins/php-impl/lib/modules/intellij.php.backend.jar"]
        }
      ]
    }
  """.trimIndent()

  private fun emptyClass(binaryName: String): ByteArray = ClassWriter(0).apply {
    visit(V1_8, ACC_PUBLIC or ACC_SUPER, binaryName, null, "java/lang/Object", null)
    visitEnd()
  }.toByteArray()
}
