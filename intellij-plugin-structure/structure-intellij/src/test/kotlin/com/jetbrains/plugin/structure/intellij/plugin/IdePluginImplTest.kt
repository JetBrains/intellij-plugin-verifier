package com.jetbrains.plugin.structure.intellij.plugin

import org.junit.Assert.assertEquals
import org.junit.Test

class IdePluginImplTest {
  @Test
  fun `clone keeps defined modules from content modules and plugin aliases`() {
    val contentModule = IdePluginImpl().apply {
      addPluginAlias("foo.alias")
    }
    val plugin = IdePluginImpl().apply {
      addPluginAlias("root.alias")
      modulesDescriptors += ModuleDescriptor.of(
        contentModule,
        Module.FileBasedModule(
          name = "foo.module",
          namespace = null,
          actualNamespace = "foo",
          loadingRule = ModuleLoadingRule.OPTIONAL,
          configFile = "foo.module.xml",
        ),
      )
    }

    val clonedPlugin = IdePluginImpl.clone(plugin, emptyList())

    assertEquals(plugin.definedModules, clonedPlugin.definedModules)
  }
}
