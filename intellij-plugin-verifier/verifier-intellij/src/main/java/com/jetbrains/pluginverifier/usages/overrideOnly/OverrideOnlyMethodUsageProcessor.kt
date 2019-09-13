package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.results.reference.SymbolicReference
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.findAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class OverrideOnlyMethodUsageProcessor(private val overrideOnlyRegistrar: OverrideOnlyRegistrar) : ApiUsageProcessor {
  override fun processApiUsage(
      apiReference: SymbolicReference,
      resolvedMember: ClassFileMember,
      usageLocation: Location,
      context: VerificationContext
  ) {
    if (resolvedMember is Method && resolvedMember.isOverrideOnlyMethod()) {
      overrideOnlyRegistrar.registerOverrideOnlyMethodUsage(OverrideOnlyMethodUsage(apiReference as MethodReference, resolvedMember.location, usageLocation))
    }
  }

  private fun Method.isOverrideOnlyMethod(): Boolean =
      runtimeInvisibleAnnotations.findAnnotation(overrideOnlyAnnotationName) != null
          || containingClassFile.runtimeInvisibleAnnotations.findAnnotation(overrideOnlyAnnotationName) != null

  private companion object {
    const val overrideOnlyAnnotationName = "org/jetbrains/annotations/ApiStatus\$OverrideOnly"
  }
}