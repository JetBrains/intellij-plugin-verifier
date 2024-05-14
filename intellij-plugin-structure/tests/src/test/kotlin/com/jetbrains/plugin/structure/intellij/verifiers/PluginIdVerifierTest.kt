package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.intellij.beans.PluginBean
import com.jetbrains.plugin.structure.intellij.beans.PluginVendorBean
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

private const val DESCRIPTOR_PATH = "plugin.xml"

class PluginIdVerifierTest {
  private lateinit var verifier: PluginIdVerifier

  private lateinit var problems: MutableList<PluginProblem>

  private val problemRegistrar = ProblemRegistrar {
    problems += it
  }

  @Before
  fun setUp() {
    verifier = PluginIdVerifier()
    problems = mutableListOf()
  }

  @Test
  fun `plugin by JetBrains has no issues`() {
    val comIntellijPlugin = plugin("com.intellij", "JetBrains")
    val ideaCorePlugin = plugin("IDEA CORE", "JetBrains")

    verifier.verify(comIntellijPlugin, DESCRIPTOR_PATH, problemRegistrar)
    verifier.verify(ideaCorePlugin, DESCRIPTOR_PATH, problemRegistrar)

    Assert.assertTrue(problems.isEmpty())
  }


  @Test
  fun `plugin by 3rd party is disallowed`() {
    val illegalId = "org.jetbrains"
    val illegalPlugin = plugin(illegalId, "Third Party Inc.")

    verifier.verify(illegalPlugin, DESCRIPTOR_PATH, problemRegistrar)

    Assert.assertEquals(1, problems.size)
    val problem = problems[0]
    Assert.assertEquals(
      "Invalid plugin descriptor 'plugin.xml'. The plugin ID '$illegalId' has a prefix 'org.jetbrains' that is not allowed.",
      problem.message
    )
  }

  @Test
  fun `plugin with generic ID is disallowed`() {
    val genericId = "com.example.genericplugin"
    val examplePlugin = plugin(genericId, "Generic Plugin Vendor Inc.")

    verifier.verify(examplePlugin, DESCRIPTOR_PATH, problemRegistrar)

    Assert.assertEquals(1, problems.size)
    val problem = problems[0]
    Assert.assertEquals(
      "Invalid plugin descriptor 'plugin.xml'. The plugin ID '$genericId' has a prefix 'com.example' that is not allowed.",
      problem.message
    )
  }

  @Test
  fun `plugin with product name in ID is disallowed`() {
    PRODUCT_ID_RESTRICTED_WORDS.forEach {
      val pluginId = "vendor.$it.quickride"
      val plugin = plugin(pluginId, "Third Party Inc.")

      verifier.verify(plugin, DESCRIPTOR_PATH, problemRegistrar)

      Assert.assertEquals(1, problems.size)
      val problem = problems[0]
      Assert.assertEquals("Invalid plugin descriptor 'plugin.xml'. The plugin ID '$pluginId' should not include the word '$it'.", problem.message)

      problems.clear()
    }
  }

  @Test
  fun `plugin with multiple case-sensitive names in ID is disallowed`() {
    val pluginId = "vendor.DataLore.DataGrip"
    val plugin = plugin(pluginId, "Third Party Inc.")
    verifier.verify(plugin, DESCRIPTOR_PATH, problemRegistrar)

    Assert.assertEquals(2, problems.size)
    val dataLoreProblem = problems[0]
    Assert.assertEquals(
      "Invalid plugin descriptor 'plugin.xml'. The plugin ID '$pluginId' should not include the word 'DataLore'.",
      dataLoreProblem.message
    )

    val dataGripProblem = problems[1]
    Assert.assertEquals(
      "Invalid plugin descriptor 'plugin.xml'. The plugin ID '$pluginId' should not include the word 'DataGrip'.",
      dataGripProblem.message
    )
  }

  private fun plugin(pluginId: String, pluginVendor: String): PluginBean {
    return PluginBean().apply {
      id = pluginId
      vendor = PluginVendorBean()
      vendor.name = pluginVendor
    }
  }

  @After
  fun tearDown() {
    problems.clear()
  }

}