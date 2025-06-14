/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.pluginverifier.plugin.DefaultPluginDetailsProvider
import com.jetbrains.pluginverifier.tests.BaseBytecodeTest
import com.jetbrains.pluginverifier.tests.mocks.MockSinglePluginDetailsCache
import com.jetbrains.pluginverifier.tests.mocks.bundledPlugin
import com.jetbrains.pluginverifier.tests.mocks.ideaPlugin
import com.jetbrains.pluginverifier.tests.mocks.withRootElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val JSON_PLUGIN_ID = "com.intellij.modules.json"


class BundledPluginDependencyFinderTest : BaseBytecodeTest() {
  private val jsonPlugin
    get() = bundledPlugin {
      id = JSON_PLUGIN_ID
      artifactName = "json"
      descriptorContent = ideaPlugin(
        pluginId = JSON_PLUGIN_ID,
        pluginName = "JSON",
        vendor = "JetBrains s.r.o."
      ).withRootElement()
    }


  @Test
  fun `plugin that declares itself as a module is resolved`() {
    val detailsProvider = DefaultPluginDetailsProvider(temporaryFolder.newFolder("extracted-plugins").toPath())
    val cache = MockSinglePluginDetailsCache(supportedPluginId = JSON_PLUGIN_ID, pluginDetailsProvider = detailsProvider)

    val ide = buildIdeWithBundledPlugins(bundledPlugins = listOf(jsonPlugin))
    val finder = BundledPluginDependencyFinder(ide)

    val dependencyResult = finder.findPluginDependency(JSON_PLUGIN_ID, true)
    assertTrue("Dependency must be 'FoundPljugin', but is '${dependencyResult.javaClass}'", dependencyResult is DependencyFinder.Result.FoundPlugin)
    dependencyResult as DependencyFinder.Result.FoundPlugin
    assertEquals(JSON_PLUGIN_ID, dependencyResult.plugin.pluginId)
  }
}