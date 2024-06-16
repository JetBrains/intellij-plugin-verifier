package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class ProductInfoBasedIdeManagerTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var ideRoot: Path

  @Before
  fun setUp() {
    ideRoot = MockIdeBuilder(temporaryFolder).buildIdeaDirectory()
  }

  @Test
  fun name() {
    val ideManager = ProductInfoBasedIdeManager()
    val ide = ideManager.createIde(Path.of("/Users/novotnyr/projects/jetbrains/platforms/IU-242.10180.25"))
    println(ide)
  }

  @Test
  fun `create IDE manager from mock IDE`() {
    val ideManager = ProductInfoBasedIdeManager()
    val ide = ideManager.createIde(ideRoot)

    assertEquals(2, ide.bundledPlugins.size)
    val uiPlugin = ide.getPluginById("intellij.notebooks.ui")
    assertNotNull(uiPlugin)
    assertTrue(uiPlugin is IdeModule)
    with(uiPlugin as IdeModule) {
      assertEquals(1, classpath.size)
      assertEquals(1, moduleDependencies.size)
      assertEquals(1, resources.size)
    }

    val visualizationPlugin = ide.getPluginById("intellij.notebooks.visualization")
    assertNotNull(visualizationPlugin)
    assertTrue(visualizationPlugin is IdeModule)

    with(visualizationPlugin as IdeModule) {
      assertEquals(1, classpath.size)
      assertEquals(1, moduleDependencies.size)
      assertEquals(1, resources.size)
    }
  }
}