package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginProblem.Level.WARNING
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
    val reusingDependency1 = "dep-one"
    val reusingDependency2 = "dep-two"
    val dependencies = listOf(
            dependency(reusingDependency1, reusedConfig),
            dependency(reusingDependency2, reusedConfig),
            dependency("dep-three", "config.xml")
    )

    val verifier = ReusedDescriptorVerifier("plugin.xml")
    verifier.verify(dependencies) { problem ->
      Assert.assertTrue(problem is ReusedDescriptorInMultipleDependencies)
      val p = problem as ReusedDescriptorInMultipleDependencies
      Assert.assertEquals(WARNING, p.level)
      Assert.assertEquals(2, p.dependencies.size)
      val expectedMessage = "Invalid plugin descriptor 'plugin.xml'. Multiple dependencies (2) use the same config-file " +
                            "attribute value 'reused-config-file': [$reusingDependency1, $reusingDependency2]."
      Assert.assertEquals(expectedMessage, p.message)
    }
  }

  private fun dependency(id: String, configFile: String): PluginDependencyBean {
    val dependency = PluginDependencyBean()
    dependency.dependencyId = id
    dependency.configFile = configFile
    return dependency
  }
}