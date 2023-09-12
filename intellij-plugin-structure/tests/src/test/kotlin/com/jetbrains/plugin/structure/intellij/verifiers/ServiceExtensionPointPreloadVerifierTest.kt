package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginContentDescriptor.*
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test


private const val PLUGIN_ID = "com.example.thirdparty"
private const val PLUGIN_VENDOR = "PluginIndustries s.r.o."
private const val MESSAGE_TEMPLATE = "Service preloading is deprecated in the <%s> " +
  "element. Consider removing the 'preload' attribute and migrating to listeners, " +
  "see https://plugins.jetbrains.com/docs/intellij/plugin-listeners.html"

class ServiceExtensionPointPreloadVerifierTest {
  private lateinit var verifier: ServiceExtensionPointPreloadVerifier

  private lateinit var problems: MutableList<PluginProblem>

  private val problemRegistrar = ProblemRegistrar {
    problems += it
  }

  @Before
  fun setUp() {
    verifier = ServiceExtensionPointPreloadVerifier()
    problems = mutableListOf()
  }


  @Test
  fun `has a single project service that is preloaded`() {
    val idePlugin = IdePluginImpl().apply {
      pluginId = PLUGIN_ID
      vendor = PLUGIN_VENDOR
      projectContainerDescriptor.services += mockServiceDescriptor(PreloadMode.TRUE, ServiceType.PROJECT)
    }
    verifier.verify(idePlugin, problemRegistrar)
    assertEquals(1, problems.size)
    val problem = problems[0]
    assertEquals(MESSAGE_TEMPLATE.format("com.intellij.projectService"), problem.message)
  }

  @Test
  fun `has a single project service that is not preloaded`() {
    val idePlugin = IdePluginImpl().apply {
      pluginId = PLUGIN_ID
      vendor = PLUGIN_VENDOR
      projectContainerDescriptor.services += mockServiceDescriptor(PreloadMode.FALSE, ServiceType.PROJECT)
    }
    verifier.verify(idePlugin, problemRegistrar)
    Assert.assertTrue(problems.isEmpty())
  }


  @Test
  fun `has a single application service that is preloaded`() {
    val idePlugin = IdePluginImpl().apply {
      pluginId = PLUGIN_ID
      vendor = PLUGIN_VENDOR
      appContainerDescriptor.services += mockServiceDescriptor(PreloadMode.TRUE, ServiceType.APPLICATION)
    }
    verifier.verify(idePlugin, problemRegistrar)
    assertEquals(1, problems.size)
    val problem = problems[0]
    assertEquals(MESSAGE_TEMPLATE.format("com.intellij.applicationService"), problem.message)
  }

  @Test
  fun `has a single module service that is preloaded`() {
    val idePlugin = IdePluginImpl().apply {
      pluginId = PLUGIN_ID
      vendor = PLUGIN_VENDOR
      moduleContainerDescriptor.services += mockServiceDescriptor(PreloadMode.TRUE, ServiceType.MODULE)
    }
    verifier.verify(idePlugin, problemRegistrar)
    assertEquals(1, problems.size)
    val problem = problems[0]
    assertEquals(MESSAGE_TEMPLATE.format("com.intellij.moduleService"), problem.message)
  }


  private fun mockServiceDescriptor(preload: PreloadMode, serviceType: ServiceType) =
    ServiceDescriptor(serviceImplementation = "com.jetbrains.mock.ServiceImpl", preload = preload,
      serviceInterface = "com.jetbrains.mock.Service", type = serviceType,
      testServiceImplementation = null, headlessImplementation = null, overrides = false,
      configurationSchemaKey = null, client = null, os = null)
}