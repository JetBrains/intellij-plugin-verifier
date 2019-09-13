package com.jetbrains.pluginverifier.usages.internal

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.results.reference.SymbolicReference
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class InternalApiUsageProcessor(private val internalApiRegistrar: InternalApiUsageRegistrar) : ApiUsageProcessor {
  override fun processApiUsage(
      apiReference: SymbolicReference,
      resolvedMember: ClassFileMember,
      usageLocation: Location,
      context: VerificationContext
  ) {
    if (resolvedMember.isInternalApi(context.classResolver)
        && resolvedMember.containingClassFile.classFileOrigin != usageLocation.containingClass.classFileOrigin
    ) {
      when (resolvedMember) {
        is ClassFile -> {
          internalApiRegistrar.registerInternalApiUsage(
              InternalClassUsage(apiReference as ClassReference, resolvedMember.location, usageLocation)
          )
        }
        is Method -> {
          internalApiRegistrar.registerInternalApiUsage(
              InternalMethodUsage(apiReference as MethodReference, resolvedMember.location, usageLocation)
          )
        }
        is Field -> {
          internalApiRegistrar.registerInternalApiUsage(
              InternalFieldUsage(apiReference as FieldReference, resolvedMember.location, usageLocation)
          )
        }
      }
    }
  }
}