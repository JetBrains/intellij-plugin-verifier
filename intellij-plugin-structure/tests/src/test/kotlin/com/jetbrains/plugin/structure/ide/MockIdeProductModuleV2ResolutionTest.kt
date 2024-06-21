package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class MockIdeProductModuleV2ResolutionTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var ideRoot: Path

  @Before
  fun setUp() {
    ideRoot = MockIdeBuilder(temporaryFolder).buildIdeaDirectory()
  }

  @Test
  fun `IDE plugin manager resolves a module from platform modules`() {
    val pluginCreationResult = IdePluginManager.createManager()
      .createBundledModule(
        ideRoot.resolve("lib").resolve("modules").resolve("intellij.notebooks.ui.jar"),
        IdeVersion.createIdeVersion("IU-242.10180.25"),
        "/intellij.notebooks.ui.xml"
      )
    assertTrue(pluginCreationResult is PluginCreationSuccess)
    val plugin = (pluginCreationResult as PluginCreationSuccess).plugin
    println(plugin)
  }

  @Test
  fun `IDE plugin manager resolves a module from platform modules including module dependencies`() {
    val pluginCreationResult = IdePluginManager.createManager()
      .createBundledModule(
        ideRoot.resolve("lib").resolve("modules").resolve("intellij.notebooks.visualization.jar"),
        IdeVersion.createIdeVersion("IU-242.10180.25"),
        "/intellij.notebooks.visualization.xml"
      )
    assertTrue(pluginCreationResult is PluginCreationSuccess)
    val plugin = (pluginCreationResult as PluginCreationSuccess).plugin

    with(plugin.definedModules) {
      assertEquals(1, size)
      assertEquals("com.intellij.modules.notebooks.visualization", first())
    }
    with(plugin.dependencies) {
      assertEquals(1, size)
      val notebooksUiModule = first()
      assertEquals("intellij.notebooks.ui", notebooksUiModule.id)
      assertTrue(notebooksUiModule.isModule)
    }
    with(plugin.extensions) {
      assertEquals(1, size)
      assertEquals(1, this["com.intellij.notificationGroup"]?.size)
    }
  }
}

