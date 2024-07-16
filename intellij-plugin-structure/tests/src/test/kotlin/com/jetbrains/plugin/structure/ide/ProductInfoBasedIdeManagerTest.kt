package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path
import java.time.LocalDate

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
  fun `create IDE manager from mock IDE`() {
    val ideManager = ProductInfoBasedIdeManager()
    val ide = ideManager.createIde(ideRoot)

    assertEquals(5, ide.bundledPlugins.size)
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

    val javaFeaturesTrainer = ide.getPluginById("intellij.java.featuresTrainer")
    assertNotNull(javaFeaturesTrainer)
    assertTrue(javaFeaturesTrainer is IdeModule)
    with(javaFeaturesTrainer as IdeModule) {
      assertEquals(1, classpath.size)
      assertEquals(0, moduleDependencies.size)
      assertEquals(1, resources.size)
    }

    val ideCore = ide.getPluginById("com.intellij")
    assertNotNull(ideCore)
    with(ideCore!!) {
      assertEquals(4, definedModules.size)
    }

    val codeWithMe = ide.getPluginById("com.jetbrains.codeWithMe")
    assertNotNull(codeWithMe)
    with(codeWithMe!!) {
      assertNotNull(productDescriptor)
      val productDescriptor = productDescriptor!!
      assertEquals(LocalDate.of(4000, 1, 1), productDescriptor.releaseDate)
    }
  }

  @Test
  fun `create nonIDEA IDE manager from mock IDE`() {
    val ideManager = ProductInfoBasedIdeManager()
    val ideRoot = MockRiderBuilder(temporaryFolder).buildIdeaDirectory()
    val ide = ideManager.createIde(ideRoot)

    val ideCore = ide.getPluginById("com.intellij")
    assertNotNull(ideCore)
    with(ideCore!!) {
      with(definedModules) {
        assertEquals(1, size)
        assertEquals("com.intellij.modules.rider", definedModules.first())
      }
    }
    val riderModule = ide.getPluginByModule("com.intellij.modules.rider")
    assertNotNull(riderModule)
    riderModule!!
    assertTrue(ideCore.pluginId == riderModule.pluginId)
  }
}