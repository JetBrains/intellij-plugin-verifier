package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.intellij.beans.PluginBean
import com.jetbrains.plugin.structure.intellij.beans.PluginVendorBean
import org.junit.Assert
import org.junit.Before
import org.junit.Test

private const val DESCRIPTOR_PATH = "plugin.xml"

class PluginIdVerifierTest {
  private lateinit var verifier: PluginIdVerifier

  @Before
  fun setUp() {
    verifier = PluginIdVerifier()
  }

  @Test
  fun `plugin by JetBrains has no issues`() {
    val problems = mutableListOf<PluginProblem>()

    val comIntellijPlugin = newPluginBean("com.intellij", "JetBrains")
    val ideaCorePlugin = newPluginBean("IDEA CORE", "JetBrains")

    val problemConsumer: (PluginProblem) -> Unit = {
      problems += it
    }
    verifier.verify(comIntellijPlugin, DESCRIPTOR_PATH, problemConsumer)
    verifier.verify(ideaCorePlugin, DESCRIPTOR_PATH, problemConsumer)

    Assert.assertTrue(problems.isEmpty())
  }


  @Test
  fun `plugin by 3rd party is disallowed`() {
    val problems = mutableListOf<PluginProblem>()

    val illegalId = "org.jetbrains"

    val illegalPlugin = newPluginBean(illegalId, "Third Party Inc.")

    val problemConsumer: (PluginProblem) -> Unit = {
      problems += it
    }
    verifier.verify(illegalPlugin, DESCRIPTOR_PATH, problemConsumer)

    Assert.assertEquals(1, problems.size)
    val problem = problems[0]
    Assert.assertEquals("Invalid plugin descriptor 'id': " +
      "Plugin ID '$illegalId' has an illegal prefix 'org.jetbrains'. " +
      "See https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html#idea-plugin__id", problem.message)
  }

  @Test
  fun `plugin with generic ID is disallowed`() {
    val problems = mutableListOf<PluginProblem>()

    val genericId = "com.example.genericplugin"

    val orgJetBrainsPlugin = newPluginBean(genericId, "Generic Plugin Vendor Inc.")

    val problemConsumer: (PluginProblem) -> Unit = {
      problems += it
    }
    verifier.verify(orgJetBrainsPlugin, DESCRIPTOR_PATH, problemConsumer)

    Assert.assertEquals(1, problems.size)
    val problem = problems[0]
    Assert.assertEquals("Invalid plugin descriptor 'id': " +
      "Plugin ID '$genericId' has an illegal prefix 'com.example'. " +
      "See https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html#idea-plugin__id", problem.message)
  }

  @Test
  fun `plugin with product name in ID is disallowed`() {
    val problems = mutableListOf<PluginProblem>()

    val riderPlugin = newPluginBean("vendor.rider.quickride", "Third Party Inc.")

    val problemConsumer: (PluginProblem) -> Unit = {
      problems += it
    }
    verifier.verify(riderPlugin, DESCRIPTOR_PATH, problemConsumer)

    Assert.assertEquals(1, problems.size)
    val problem = problems[0]
    Assert.assertEquals("Plugin ID specified in plugin.xml should not contain the word 'rider'", problem.message)
  }

  @Test
  fun `plugin with partial product name in ID is allowed`() {
    val problems = mutableListOf<PluginProblem>()

    // contains 'space' as a product name, but this is allowed
    val genericId = "vendor.spacetrip"

    val plugin = newPluginBean(genericId, "Space Trip Vendor Inc.")

    val problemConsumer: (PluginProblem) -> Unit = {
      problems += it
    }
    verifier.verify(plugin, DESCRIPTOR_PATH, problemConsumer)

    Assert.assertTrue(problems.isEmpty())
  }

  private fun newPluginBean(pluginId: String, pluginVendor: String): PluginBean {
    return PluginBean().apply {
      id = pluginId
      vendor = PluginVendorBean()
      vendor.name = pluginVendor
    }
  }

}