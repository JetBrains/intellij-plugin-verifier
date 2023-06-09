/*
 * Copyright 2000-2023 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginVendorsTest {
  @Test
  fun `has single vendor which is 3rd party`() {
    val idePlugin = IdePluginImpl().apply {
      pluginId = "com.example.thirdparty"
      vendor = "PluginIndustries s.r.o."
    }
    val isJetBrainsPlugin = PluginVendors.isDevelopedByJetBrains(idePlugin)
    assertFalse(isJetBrainsPlugin)
  }

  @Test
  fun `has single vendor which is JetBrains`() {
    val idePlugin = IdePluginImpl().apply {
      pluginId = "com.intellij.internal"
      vendor = "JetBrains s.r.o."
    }
    val isJetBrainsPlugin = PluginVendors.isDevelopedByJetBrains(idePlugin)
    assertTrue(isJetBrainsPlugin)
  }

  @Test
  fun `has multiple vendors and one of them is JetBrains`() {
    val idePlugin = IdePluginImpl().apply {
      pluginId = "com.intellij"
      vendor = "JetBrains s.r.o., PluginIndustries s.r.o."
    }
    val isJetBrainsPlugin = PluginVendors.isDevelopedByJetBrains(idePlugin)
    assertTrue(isJetBrainsPlugin)
  }

  @Test
  fun `has multiple vendors and none of those is JetBrains`() {
    val idePlugin = IdePluginImpl().apply {
      pluginId = "com.intellij.someplugin"
      vendor = "PluginIndustries s.r.o., PluginFactory s.r.o."
    }
    val isJetBrainsPlugin = PluginVendors.isDevelopedByJetBrains(idePlugin)
    assertFalse(isJetBrainsPlugin)
  }
}