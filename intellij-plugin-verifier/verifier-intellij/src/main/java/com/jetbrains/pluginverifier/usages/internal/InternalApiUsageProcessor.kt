package com.jetbrains.pluginverifier.usages.internal

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class InternalApiUsageProcessor : ApiUsageProcessor {
  override fun processApiUsage(classFileMember: ClassFileMember, usageLocation: Location, context: VerificationContext) {
    if (context is InternalApiRegistrar
        && classFileMember.isInternalApi(context)
        && classFileMember.containingClassFile.classFileOrigin != usageLocation.containingClass.classFileOrigin
    ) {
      when (classFileMember) {
        is ClassFile -> {
          context.registerInternalApiUsage(
              InternalClassUsage(classFileMember.location, usageLocation)
          )
        }
        is Method -> {
          context.registerInternalApiUsage(
              InternalMethodUsage(classFileMember.location, usageLocation)
          )
        }
        is Field -> {
          context.registerInternalApiUsage(
              InternalFieldUsage(classFileMember.location, usageLocation)
          )
        }
      }
    }
  }
}