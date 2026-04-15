package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.DependenciesTest
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Paths

class ProductInfoIdeManagerLazyLoadingTest {
  @Test
  fun `create IDE manager from IDE dump and do not eagerly load plugins unless necessary`() {
    val ideResourceLocation = "/ide-dumps/IC-242.24807.4"
    val ideUrl = DependenciesTest::class.java.getResource(ideResourceLocation)
    assertNotNull("Dumped IDE not found in the resources [$ideResourceLocation]", ideUrl)
    ideUrl!!
    val ideRoot = Paths.get(ideUrl.toURI())

    val ide = ProductInfoBasedIdeManager(MissingLayoutFileMode.SKIP_CLASSPATH)
      .createIde(ideRoot)
    assertTrue(ide is ProductInfoAware)
    ide as ProductInfoBasedIde
    ide.assertEfficiency()
    assertFalse(ide.hasBundledPlugin("UNKNOWN PLUGIN"))
    ide.assertEfficiency()
    assertTrue(ide.hasBundledPlugin("com.intellij.java"))
    ide.assertEfficiency()
    val javaPlugin = ide.findPluginById("com.intellij.java")
    assertNotNull(javaPlugin)
    assertTrue(ide.isPluginCollectionLoaded())
  }

  private fun ProductInfoBasedIde.assertEfficiency() {
    assertFalse(isPluginCollectionLoaded())
  }
}