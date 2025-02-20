package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginV2Dependency
import com.jetbrains.pluginverifier.tests.BasePluginTest
import com.jetbrains.pluginverifier.tests.mocks.MockDependencyFinder
import com.jetbrains.pluginverifier.tests.mocks.RuleBasedDependencyFinder
import com.jetbrains.pluginverifier.tests.mocks.buildCoreIde
import com.jetbrains.pluginverifier.tests.mocks.buildIdePlugin
import com.jetbrains.pluginverifier.tests.mocks.descriptor
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test

private const val PROVISIONER_PLUGIN_ID = "com.intellij.platform.ide.provisioner"

class CompositeDependencyFinderTest : BasePluginTest() {
  lateinit var ide: Ide

  lateinit var dependencyFinder: DependencyFinder

  @Before
  fun setUp() {
    ide = ideaPath.buildCoreIde()

    val finder1 = RuleBasedDependencyFinder.create(
      ide,
      RuleBasedDependencyFinder.Rule(PROVISIONER_PLUGIN_ID, provisionerPlugin())
    )
    val finder2 = MockDependencyFinder()
    dependencyFinder = CompositeDependencyFinder(listOf(finder1, finder2))
  }

  @Test
  fun `simple dependency is resolved`() {
    val result = dependencyFinder.findPluginDependency(PluginV2Dependency(PROVISIONER_PLUGIN_ID))
    assertTrue(result is DependencyFinder.Result.DetailsProvided)
  }

  private fun provisionerPlugin(): IdePlugin {
    val descriptor = ideaPlugin(PROVISIONER_PLUGIN_ID, "Provisioner")
    return newJar("provisioner").buildIdePlugin {
      descriptor(descriptor)
    }
  }
}