package com.jetbrains.plugin.structure.intellij.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path

class ClasspathTest {
  @Test
  fun `classpath is created`() {
    val pluginPath = Path.of("lib", "plugin.jar")
    val modulePath = Path.of("lib", "modules", "module.jar")

    val cp = Classpath.of(listOf(pluginPath, modulePath))
    assertEquals(2, cp.size)
    assertTrue("Classpath must contain the plugin artifact",cp.entries.any { it.path == pluginPath })
    assertTrue("Classpath must contain the module artifact", cp.entries.any { it.path == modulePath })
  }

  @Test
  fun `classpath paths are retrieved without duplicates`() {
    val pluginPath = Path.of("lib", "plugin.jar")
    val modulePath = Path.of("lib", "modules", "module.jar")
    val v2ModulePathAsADuplicate = Path.of("lib", "modules", "module.jar")

    val cp = Classpath.of(listOf(pluginPath, modulePath, v2ModulePathAsADuplicate))

    with(cp.paths) {
      assertEquals(2, size)
      assertTrue("Classpath paths must contain the plugin artifact",any { it == pluginPath })
      assertTrue("Classpath paths must contain the module artifact", any { it == modulePath })
    }
  }

  @Test
  fun `duplicate paths are retrieved`() {
    val pluginPath = Path.of("lib", "plugin.jar")
    val duplicatePluginPath = Path.of("lib", "plugin.jar")
    val modulePath = Path.of("lib", "modules", "module.jar")

    val cp = Classpath.of(listOf(pluginPath, duplicatePluginPath, modulePath))

    with(cp.paths) {
      assertEquals(2, size)
      assertTrue("Classpath paths must contain the plugin artifact",any { it == pluginPath })
      assertTrue("Classpath paths must contain the module artifact", any { it == modulePath })
    }
  }
}