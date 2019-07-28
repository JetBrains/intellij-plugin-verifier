package com.jetbrains.pluginverifier.usages.internal

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class InternalApiUsageProcessor(private val internalApiRegistrar: InternalApiUsageRegistrar) : ApiUsageProcessor {
  override fun processApiUsage(classFileMember: ClassFileMember, usageLocation: Location, context: VerificationContext) {
    if (classFileMember.isInternalApi(context)
        && classFileMember.containingClassFile.classFileOrigin != usageLocation.containingClass.classFileOrigin
    ) {
      when (classFileMember) {
        is ClassFile -> {
          internalApiRegistrar.registerInternalApiUsage(
              InternalClassUsage(classFileMember.location, usageLocation)
          )
        }
        is Method -> {
          internalApiRegistrar.registerInternalApiUsage(
              InternalMethodUsage(classFileMember.location, usageLocation)
          )
        }
        is Field -> {
          internalApiRegistrar.registerInternalApiUsage(
              InternalFieldUsage(classFileMember.location, usageLocation)
          )
        }
      }
    }
  }
}