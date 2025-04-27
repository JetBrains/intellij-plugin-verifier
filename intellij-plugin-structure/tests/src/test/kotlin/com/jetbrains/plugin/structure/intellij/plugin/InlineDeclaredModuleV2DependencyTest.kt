package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.mocks.MockIdePlugin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InlineDeclaredModuleV2DependencyTest {
  /**
   * See:
   * ```
   * <idea-plugin
   *   <id>org.toml.lang</id>
   *   <content>
   *     <module name="intellij.toml.json"><![CDATA[
   *       <idea-plugin>
   *         <dependencies>
   *           <plugin id="com.intellij.modules.json" />
   * ```
   */
  @Test
  fun `created inline dependency is consistent`() {
    val tomlPlugin = MockIdePlugin(pluginId = "org.toml.lang")
    val contentModuleContent = """
    <idea-plugin>
      <dependencies>
        <plugin id="com.intellij.modules.json"
      </dependencies>
    </idea-plugin>              
    """.trimIndent()

    val moduleLoadingRule = ModuleLoadingRule.OPTIONAL

    val dependency = InlineDeclaredModuleV2Dependency.of("com.intellij.modules.json",
      moduleLoadingRule,
      contentModuleOwner = tomlPlugin,
      contentModuleReference = Module.InlineModule("intellij.toml.json",
        moduleLoadingRule, contentModuleContent)
      )
    with(dependency) {
      assertEquals("com.intellij.modules.json", id)
      assertTrue(isOptional)
      assertEquals(tomlPlugin.pluginId, contentModuleOwnerId)
      assertEquals("intellij.toml.json", dependerContentModuleId)
      assertEquals("com.intellij.modules.json (module, v2, specified in content module 'intellij.toml.json' of '${tomlPlugin.pluginId}')", toString())
    }
  }
}