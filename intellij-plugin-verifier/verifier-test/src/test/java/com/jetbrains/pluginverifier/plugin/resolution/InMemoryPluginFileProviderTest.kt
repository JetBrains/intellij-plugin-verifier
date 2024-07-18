package com.jetbrains.pluginverifier.plugin.resolution

import com.jetbrains.pluginverifier.plugin.PluginFileProvider.Result.Found
import com.jetbrains.pluginverifier.plugin.PluginFileProvider.Result.NotFound
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InMemoryPluginFileProviderTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private val plugin = PluginInfo("somePlugin", "Some Plugin", "1.0.0")
  private val dependency = PluginInfo("aDependency", "Dependency", "1.0.0")

  private lateinit var provider: InMemoryPluginFileProvider

  @Before
  fun setUp() {
    provider = InMemoryPluginFileProvider()
    provider[plugin] = temporaryFolder.createJarPath(plugin)
    provider[dependency] = temporaryFolder.createJarPath(dependency)
  }

  @Test
  fun `existing plugins are resolved`() {
    val pluginResult = provider.getPluginFile(plugin)
    assert(pluginResult is Found)

    val dependencyResult = provider.getPluginFile(dependency)
    assert(dependencyResult is Found)
  }

  @Test
  fun `unknown plugin is resolved as not found`() {
    val randomPlugin = PluginInfo("randomPlugin", "Random Plugin", "1.0.0")

    val pluginResult = provider.getPluginFile(randomPlugin)
    assert(pluginResult is NotFound)
  }
}