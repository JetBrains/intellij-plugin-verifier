package com.jetbrains.plugin.structure.intellij.platform

import com.jetbrains.plugin.structure.intellij.beans.ModuleBean
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class BundledModulesManagerTest {
  @Test
  fun `find module by name keeps the first resolved module`() {
    val first = ModuleBean("duplicate")
    val second = ModuleBean("duplicate")

    val manager = BundledModulesManager {
      listOf(first, second)
    }

    assertSame(first, manager.findModuleByName("duplicate"))
    assertNull(manager.findModuleByName("missing"))
  }
}
