package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.ReclassifiedPluginProblem
import com.jetbrains.plugin.structure.base.problems.VendorCannotBeEmpty
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class LevelRemappingPluginCreationResultResolverTest {
  @Test
  fun `remapped problem is unwrapped and remapped again`() {
    val idePlugin = IdePluginImpl().apply {
      pluginId = "com.intellij.someplugin"
      vendor = "" // deliberately empty
    }

    val pluginProblem = VendorCannotBeEmpty(PLUGIN_XML)

    val resolver = LevelRemappingPluginCreationResultResolver(IntelliJPluginCreationResultResolver(),
      unacceptableWarning<VendorCannotBeEmpty>(), unwrapRemappedProblems = true)
    val creationResult = resolver.resolve(idePlugin, listOf(
      ReclassifiedPluginProblem(PluginProblem.Level.WARNING, pluginProblem)
    ))
    assertThat(creationResult, instanceOf(PluginCreationSuccess::class.java))
    val creationSuccess = creationResult as PluginCreationSuccess
    assertThat(creationSuccess.unacceptableWarnings.size, `is`(1))
    assertThat(creationSuccess.warnings.size, `is`(0))
  }
}