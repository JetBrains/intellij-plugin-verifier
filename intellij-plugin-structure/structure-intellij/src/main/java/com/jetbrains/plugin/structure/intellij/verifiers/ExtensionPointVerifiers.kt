package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginContentDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginContentDescriptor.ServiceDescriptor
import com.jetbrains.plugin.structure.intellij.problems.ServiceExtensionPointPreloadNotSupported

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