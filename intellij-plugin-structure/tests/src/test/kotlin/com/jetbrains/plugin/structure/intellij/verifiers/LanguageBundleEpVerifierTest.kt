package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import com.jetbrains.plugin.structure.mocks.MockExtension
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private const val PLUGIN_ID = "com.example.thirdparty"
private const val PLUGIN_VENDOR = "PluginIndustries s.r.o."
private const val JETBRAINS_PLUGIN_VENDOR = "JetBrains"
private const val MESSAGE_TEMPLATE = "The extension point in the <com.intellij.languageBundle> element is internal and must be used by JetBrains only."


class LanguageBundleEpVerifierTest {
  private lateinit var verifier: LanguageBundleExtensionPointVerifier

  private lateinit var problems: MutableList<PluginProblem>

  private val problemRegistrar = ProblemRegistrar {
    problems += it
  }

  @Before
  fun setUp() {
    verifier = LanguageBundleExtensionPointVerifier()
    problems = mutableListOf()
  }

  @Test
  fun `plugin is not allowed to use languageBundle EP`() {
    val extension = MockExtension.from("languageBundle", "locale" to "en-US")

    val idePlugin = IdePluginImpl().apply {
      pluginId = PLUGIN_ID
      vendor = PLUGIN_VENDOR
      extension.apply(this)
    }
    verifier.verify(idePlugin, problemRegistrar)
    assertEquals(1, problems.size)
    val problem = problems[0]
    assertEquals(MESSAGE_TEMPLATE, problem.message)
  }

  @Test
  fun `JetBrains plugin is allowed to use languageBundle EP`() {
    val extension = MockExtension.from("languageBundle", "locale" to "en-US")

    val idePlugin = IdePluginImpl().apply {
      pluginId = PLUGIN_ID
      vendor = JETBRAINS_PLUGIN_VENDOR
      extension.apply(this)
    }
    verifier.verify(idePlugin, problemRegistrar)
    assertEquals(0, problems.size)
  }
}