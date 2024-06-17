package com.jetbrains.pluginverifier.usages.internal

import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext

class AnnotatedInternalApiUsageRegistrar(private val verificationContext: PluginVerificationContext) : InternalUsageRegistrar {
  override fun registerClass(classReference: ClassReference, apiElement: ClassLocation, usageLocation: Location) {
    register(InternalClassUsage(classReference, apiElement, usageLocation))
  }

  override fun registerMethod(
    methodReference: MethodReference,
    apiElement: MethodLocation,
    usageLocation: MethodLocation
  ) {
    register(InternalMethodUsage(methodReference, apiElement, usageLocation))
  }

  override fun registerField(fieldReference: FieldReference, apiElement: FieldLocation, usageLocation: MethodLocation) {
    register(InternalFieldUsage(fieldReference, apiElement, usageLocation))
  }

  private fun register(usage: InternalApiUsage) {
    // MP-3421 Plugin Verifier must report compatibility errors for usages of internal FUS APIs
    if (usage.apiElement.containingClass.packageName.startsWith("com/intellij/internal/statistic")
      && verificationContext.idePlugin.vendor?.contains("JetBrains", true) != true
      && verificationContext.verificationDescriptor is PluginVerificationDescriptor.IDE
      && verificationContext.verificationDescriptor.ideVersion.baselineVersion >= 211
    ) {
      verificationContext.registerProblem(InternalFusApiUsageCompatibilityProblem(usage))
    } else {
      verificationContext.registerInternalApiUsage(usage)
    }
  }
}