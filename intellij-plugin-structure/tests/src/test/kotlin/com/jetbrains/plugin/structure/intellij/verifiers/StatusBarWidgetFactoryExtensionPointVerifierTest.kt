package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import org.jdom2.Element
import org.junit.Assert
import org.junit.Before
import org.junit.Test

private const val PLUGIN_ID = "com.example.thirdparty"
private const val PLUGIN_VENDOR = "PluginIndustries s.r.o."
private const val EP_IMPLEMENTATION = "com.example.MyStatusBarWidgetFactory"

private const val MESSAGE_TEMPLATE = "Extension Point in the <com.intellij.statusBarWidgetFactory> element " +
  "must have 'id' attribute set with same value returned from the getId() method of the $EP_IMPLEMENTATION implementation."


class StatusBarWidgetFactoryExtensionPointVerifierTest {
  private lateinit var verifier: StatusBarWidgetFactoryExtensionPointVerifier

  private lateinit var problems: MutableList<PluginProblem>

  private val problemRegistrar = ProblemRegistrar {
    problems += it
  }

  @Before
  fun setUp() {
    verifier = StatusBarWidgetFactoryExtensionPointVerifier()
    problems = mutableListOf()
  }


  @Test
  fun `status bar widget factory extension does not declare ID`() {
    val epElement = Element("statusBarWidgetFactory").apply {
      setAttribute("implementation", EP_IMPLEMENTATION)
    }

    val idePlugin = IdePluginImpl().apply {
      pluginId = PLUGIN_ID
      vendor = PLUGIN_VENDOR
      extensions["com.intellij.statusBarWidgetFactory"] = mutableListOf(epElement)
    }
    verifier.verify(idePlugin, problemRegistrar)
    Assert.assertEquals(1, problems.size)
    val problem = problems[0]
    Assert.assertEquals(MESSAGE_TEMPLATE, problem.message)
  }

  @Test
  fun `status bar widget factory extension correctly declares ID and implementation`() {
    val epElement = Element("statusBarWidgetFactory").apply {
      setAttribute("id", "$PLUGIN_ID.MyStatusBarWidgetFactory")
      setAttribute("implementation", EP_IMPLEMENTATION)
    }

    val idePlugin = IdePluginImpl().apply {
      pluginId = PLUGIN_ID
      vendor = PLUGIN_VENDOR
      extensions["com.intellij.statusBarWidgetFactory"] = mutableListOf(epElement)
    }
    verifier.verify(idePlugin, problemRegistrar)
    Assert.assertEquals(0, problems.size)
  }
}