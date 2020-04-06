package com.jetbrains.pluginverifier.verifiers.services

import com.jetbrains.plugin.structure.classes.resolvers.findOriginOfType
import com.jetbrains.plugin.structure.ide.classes.IdeFileOrigin
import com.jetbrains.plugin.structure.intellij.classes.locator.PluginFileOrigin
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.presentation.toFullJavaClassName
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.ClassUsageType
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning

class ServiceConstructorInjectionDetector : ApiUsageProcessor {

  private companion object {
    val CONSTRUCTOR_INJECTION_ALLOWED_SERVICES: List<String> = listOf(
      "com/intellij/openapi/project/Project",
      "com/intellij/openapi/module/Module"
    )
  }

  override fun processClassReference(
    classReference: ClassReference,
    resolvedClass: ClassFile,
    context: VerificationContext,
    referrer: ClassFileMember,
    classUsageType: ClassUsageType
  ) {
    if (classUsageType != ClassUsageType.METHOD_PARAMETER) return
    if (resolvedClass.name in CONSTRUCTOR_INJECTION_ALLOWED_SERVICES) return
    if (!(referrer is Method && referrer.isConstructor)) return
    if (context !is PluginVerificationContext) return
    val verificationDescriptor = context.verificationDescriptor
    if (verificationDescriptor !is PluginVerificationDescriptor.IDE) return

    val plugin = context.idePlugin

    // Access to plugin services is not part of the [IdePlugin] API yet, can be changed later.
    if (plugin !is IdePluginImpl) return

    //TODO: is injection prohibited only to constructors of plugin's services?
    if (!isServiceDeclaredInPlugin(referrer.containingClassFile, plugin)) return

    val originPlugin = resolveOriginPluginOfClass(resolvedClass) as? IdePluginImpl ?: return

    if (!isServiceDeclaredInPlugin(resolvedClass, originPlugin)) return

    //1) Constructor reference
    context.registerCompatibilityWarning(
      ServiceConstructorInjectionWarning(
        referrer.location,
        classReference
      )
    )
  }

  private fun isServiceDeclaredInPlugin(classFile: ClassFile, idePlugin: IdePluginImpl): Boolean {
    val javaClassName = toFullJavaClassName(classFile.name)
    val allPluginServices = sequenceOf(
      idePlugin.appContainerDescriptor,
      idePlugin.projectContainerDescriptor,
      idePlugin.moduleContainerDescriptor
    ).flatMap { it.services.asSequence() }
    return allPluginServices.any { serviceDescriptor ->
      javaClassName == serviceDescriptor.serviceInterface
        || javaClassName == serviceDescriptor.serviceImplementation
    }
  }

  private fun resolveOriginPluginOfClass(classFile: ClassFile): IdePlugin? {
    val pluginFileOrigin = classFile.classFileOrigin.findOriginOfType<PluginFileOrigin>()
    if (pluginFileOrigin != null) return pluginFileOrigin.idePlugin
    val ideFileOrigin = classFile.classFileOrigin.findOriginOfType<IdeFileOrigin>()
    ideFileOrigin ?: return null
    return ideFileOrigin.ide.getPluginById("com.intellij")
  }
}

class ServiceConstructorInjectionWarning(
  private val constructorLocation: MethodLocation,
  private val serviceClassReference: ClassReference
) : CompatibilityWarning() {

  override val shortDescription
    get() = "Constructor injection of services is deprecated"

  //TODO: clarify + rephrase + more details + links.
  override val fullDescription
    get() = "Service ${serviceClassReference.presentableLocation} is injected into " +
      "constructor ${constructorLocation.presentableLocation} of ${constructorLocation.hostClass.presentableLocation}"
}