package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginContentDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginContentDescriptor.ServiceDescriptor
import com.jetbrains.plugin.structure.intellij.problems.ServiceExtensionPointPreloadNotSupported
import com.jetbrains.plugin.structure.intellij.problems.StatusBarWidgetFactoryExtensionPointIdMissing

/**
 * Rule: Service Extension Point preloading is deprecated.
 */
class ServiceExtensionPointPreloadVerifier {
  fun verify(plugin: IdePlugin, problemRegistrar: ProblemRegistrar) {
    val allDescriptors = plugin.appContainerDescriptor.services +
      plugin.projectContainerDescriptor.services +
      plugin.moduleContainerDescriptor.services

    allDescriptors.forEach {
      verifyDescriptor(it, problemRegistrar)
    }
  }

  private fun verifyDescriptor(serviceDescriptor: ServiceDescriptor, problemRegistrar: ProblemRegistrar) {
    if (serviceDescriptor.preload != IdePluginContentDescriptor.PreloadMode.FALSE) {
      problemRegistrar.registerProblem(ServiceExtensionPointPreloadNotSupported(serviceDescriptor.type))
    }
  }
}

/**
 * Rule: `statusBarWidgetFactory` must have an `id` attribute set.
 *
 * Furthermore, this `id` must match the value from `statusBarWidgetFactory#getId` method, but this
 * cannot be verified on the bytecode-level at this stage of verification, as it is a dynamic expression.
 */
class StatusBarWidgetFactoryExtensionPointVerifier {
  private val extensionPointName = "com.intellij.statusBarWidgetFactory"

  fun verify(plugin: IdePlugin, problemRegistrar: ProblemRegistrar) {
    val statusBarWidgetFactories = plugin.extensions[extensionPointName] ?: emptyList()
    statusBarWidgetFactories.forEach {
      val extensionId = it.getAttribute("id")
      if (extensionId == null) {
        problemRegistrar.registerProblem(StatusBarWidgetFactoryExtensionPointIdMissing())
      }
    }
  }
}