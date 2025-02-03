/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.ide.plugin.DefaultPluginIdProvider
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class BundledPluginManagerTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Before
  fun setUp() {
    val javaLibDir = temporaryFolder.newFolder("plugins", "java", "lib")
    val javaImplJar = javaLibDir.resolve("java-impl.jar").toPath()
    buildZipFile(javaImplJar) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              <id>com.intellij.java</id>
            </idea-plugin>
            """.trimIndent()
        }
      }
    }

    val javaCoverageDir = temporaryFolder.newFolder("plugins", "java-coverage", "lib")
    val javaCoverageJar = javaCoverageDir.resolve("java-coverage.jar").toPath()
    buildZipFile(javaCoverageJar) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              <name>Code Coverage for Java</name>
              <id>Coverage</id>
            </idea-plugin>
          """.trimIndent()
        }
      }
    }
  }

  @Test
  fun `plugin identifiers are retrieved`() {
    val pluginIdProvider = DefaultPluginIdProvider()
    val pluginManager = BundledPluginManager(pluginIdProvider)
    val pluginIdentifiers = pluginManager.getBundledPluginIds(idePath).map { it.pluginId }
    assertTrue(pluginIdentifiers.contains("com.intellij.java"))
    assertTrue(pluginIdentifiers.contains("Coverage"))
  }

  private val idePath: Path
    get() = temporaryFolder.root.toPath()
}