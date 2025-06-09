/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

import com.jetbrains.plugin.structure.base.plugin.IconTheme
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

private const val DEFAULT_ICON_SVG = """<svg xmlns="http://www.w3.org/2000/svg"><circle r="9" /></svg>"""
private const val DARK_ICON_SVG = """<svg xmlns="http://www.w3.org/2000/svg"><circle r="9" fill="gray" /></svg>"""

class PluginIconLoaderTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var pluginIconLoader: PluginIconLoader

  @Before
  fun setUp() {
    pluginIconLoader = PluginIconLoader()
  }

  @Test
  fun `plugin icon is loaded`() {
    val pluginDirectory: Path = temporaryFolder.newFolder("plugin").toPath()
    buildDirectory(pluginDirectory) {
      dir("META-INF") {
        file("pluginIcon.svg", DEFAULT_ICON_SVG)
      }
    }
    val icons = pluginIconLoader.load(pluginDirectory)
    with(icons) {
      assertEquals(1, size)
      val icon = first()
      assertEquals(IconTheme.DEFAULT, icon.theme)
      assertEquals("pluginIcon.svg", icon.fileName)
    }
  }

  @Test
  fun `plugin icons are loaded for default and dark mode`() {
    val pluginDirectory: Path = temporaryFolder.newFolder("plugin").toPath()
    buildDirectory(pluginDirectory) {
      dir("META-INF") {
        file("pluginIcon.svg", DEFAULT_ICON_SVG)
        file("pluginIcon_dark.svg", DARK_ICON_SVG)
      }
    }
    val icons = pluginIconLoader.load(pluginDirectory)
    with(icons) {
      assertEquals(2, size)
      with(first()) {
        assertEquals(IconTheme.DEFAULT, theme)
        assertEquals("pluginIcon.svg", fileName)
      }

      with(get(1)) {
        assertEquals(IconTheme.DARCULA, theme)
        assertEquals("pluginIcon_dark.svg", fileName)
      }
    }
  }
}