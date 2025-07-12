/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

import com.jetbrains.plugin.structure.intellij.resources.DefaultResourceResolver
import com.jetbrains.plugin.structure.mocks.MockIdePlugin
import com.jetbrains.plugin.structure.mocks.SimpleProblemRegistrar
import org.jdom2.Element
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path

private const val INTELLIJ_THEME_EXTENSION = "com.intellij.themeProvider"

class PluginThemeLoaderTest {
  @Test
  fun `two unavailable themes`() {
    val loader = PluginThemeLoader()

    val firstMissingThemeElement = Element(INTELLIJ_THEME_EXTENSION).apply {
      setAttribute("path", "/nonexistent.theme.json")
    }
    val secondMissingThemeElement = Element(INTELLIJ_THEME_EXTENSION).apply {
      setAttribute("path", "/another-nonexistent.theme.json")
    }

    val extensions = mapOf(
      INTELLIJ_THEME_EXTENSION to listOf(firstMissingThemeElement, secondMissingThemeElement)
    )
    val plugin = MockIdePlugin(extensions = extensions)

    val problemRegistrar = SimpleProblemRegistrar()
    val result = loader.load(plugin, Path.of("META-INF/plugin.xml"), DefaultResourceResolver, problemRegistrar)
    assertTrue(result is PluginThemeLoader.Result.NotFound)
    result as PluginThemeLoader.Result.NotFound

    assertEquals(2, problemRegistrar.problems.size)
  }
}