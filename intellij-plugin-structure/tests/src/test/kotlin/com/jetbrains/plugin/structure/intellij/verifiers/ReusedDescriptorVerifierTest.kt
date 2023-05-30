package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.plugin.PluginProblem.Level.WARNING
import com.jetbrains.plugin.structure.base.problems.ReusedDescriptorInMultipleDependencies
import com.jetbrains.plugin.structure.intellij.beans.PluginDependencyBean
import org.junit.Assert
import org.junit.Test

class ReusedDescriptorVerifierTest {
  @Test
  fun `config-file is not reused`() {
    val reusedConfig = "reused-config-file"
    val dependencies = listOf(
            dependency("dep-one", reusedConfig),
            dependency("dep-two", "config.xml")
    )

    val verifier = ReusedDescriptorVerifier("plugin.xml")
    val problems = mutableListOf<PluginProblem>()
    verifier.verify(dependencies) { problem -> problems += problem }
    Assert.assertTrue(problems.isEmpty())
  }

  @Test
  fun `config-file is reused in two dependencies`() {
    val reusedConfig = "reused-config-file"
    val dependencies = listOf(
            dependency("dep-one", reusedConfig),
            dependency("dep-two", reusedConfig),
            dependency("dep-three", "config.xml")
    )

    val verifier = ReusedDescriptorVerifier("plugin.xml")
    verifier.verify(dependencies) { problem ->
      Assert.assertTrue(problem is ReusedDescriptorInMultipleDependencies)
      val p = problem as ReusedDescriptorInMultipleDependencies
      Assert.assertEquals(WARNING, p.level)
      Assert.assertEquals(2, p.dependencies.size)
    }
  }

  private fun dependency(id: String, configFile: String): PluginDependencyBean {
    val dependency = PluginDependencyBean()
    dependency.dependencyId = id
    dependency.configFile = configFile
    return dependency
  }
}