/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.pluginverifier.plugin.DefaultPluginDetailsProvider
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
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
    val finder = BundledPluginDependencyFinder(ide, cache)

    val dependencyResult = finder.findPluginDependency(JSON_PLUGIN_ID, true)
    assertTrue("Dependency must be 'DetailsProvided', but is '${dependencyResult.javaClass}'", dependencyResult is DependencyFinder.Result.DetailsProvided)
    dependencyResult as DependencyFinder.Result.DetailsProvided
    val pluginDetails = dependencyResult.pluginDetailsCacheResult
    assertTrue("Plugin details must be 'Provided', but is '${pluginDetails.javaClass}'", pluginDetails is PluginDetailsCache.Result.Provided)
    pluginDetails as PluginDetailsCache.Result.Provided
    assertEquals(JSON_PLUGIN_ID, pluginDetails.pluginDetails.idePlugin.pluginId)
    assertEquals(JSON_PLUGIN_ID, pluginDetails.pluginDetails.pluginInfo.pluginId)
  }
}