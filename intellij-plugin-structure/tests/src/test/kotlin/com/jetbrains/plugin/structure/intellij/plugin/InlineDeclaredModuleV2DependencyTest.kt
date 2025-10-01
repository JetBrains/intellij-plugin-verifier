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

    val dependency = InlineDeclaredModuleV2Dependency.onPlugin("com.intellij.modules.json",
      moduleLoadingRule,
      contentModuleOwner = tomlPlugin,
      contentModuleReference = Module.InlineModule(
        "intellij.toml.json",
        namespace = null, actualNamespace = "jetbrains", moduleLoadingRule, contentModuleContent
      )
      )
    with(dependency) {
      assertEquals("com.intellij.modules.json", id)
      assertTrue(isOptional)
      assertEquals(tomlPlugin.pluginId, contentModuleOwnerId)
      assertEquals("intellij.toml.json", dependerContentModuleId)
      assertEquals("dependency on plugin 'com.intellij.modules.json' declared in content module 'intellij.toml.json' of 'org.toml.lang'", toString())
    }
  }


  @Test
  fun `create inline dependency on a module`() {
    val thymeleafPlugin = MockIdePlugin(pluginId = "com.intellij.thymeleaf")
    val contentModuleContent = """
      <idea-plugin package="com.intellij.thymeleaf.spring">
        <dependencies>
          <module name="intellij.spring.el" />
        </dependencies>
      </idea-plugin>              
      """.trimIndent()

    val moduleLoadingRule = ModuleLoadingRule.OPTIONAL

    val dependency = InlineDeclaredModuleV2Dependency.onModule("intellij.spring.el",
      moduleLoadingRule,
      contentModuleOwner = thymeleafPlugin,
      contentModuleReference = Module.InlineModule(
        "intellij.thymeleaf/spring-el",
        namespace = null, actualNamespace = "jetbrains", moduleLoadingRule, contentModuleContent
      )
    )
    with(dependency) {
      assertEquals("intellij.spring.el", id)
      assertTrue(isOptional)
      assertEquals(thymeleafPlugin.pluginId, contentModuleOwnerId)
      assertEquals("intellij.thymeleaf/spring-el", dependerContentModuleId)
      assertEquals("dependency on module 'intellij.spring.el' declared in content module 'intellij.thymeleaf/spring-el' of 'com.intellij.thymeleaf'", toString())
    }
  }
}