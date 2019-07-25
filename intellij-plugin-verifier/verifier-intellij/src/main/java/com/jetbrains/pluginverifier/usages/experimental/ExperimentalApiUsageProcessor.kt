package com.jetbrains.pluginverifier.usages.experimental

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class ExperimentalApiUsageProcessor : ApiUsageProcessor {
  override fun processApiUsage(classFileMember: ClassFileMember, usageLocation: Location, context: VerificationContext) {
    if (context is ExperimentalApiRegistrar
        && classFileMember.isExperimentalApi(context)
        && classFileMember.containingClassFile.classFileOrigin != usageLocation.containingClass.classFileOrigin
    ) {
      when (classFileMember) {
        is ClassFile -> {
          context.registerExperimentalApiUsage(
              ExperimentalClassUsage(classFileMember.location, usageLocation)
          )
        }
        is Method -> {
          context.registerExperimentalApiUsage(
              ExperimentalMethodUsage(classFileMember.location, usageLocation)
          )
        }
        is Field -> {
          context.registerExperimentalApiUsage(
              ExperimentalFieldUsage(classFileMember.location, usageLocation)
          )
        }
      }
    }
  }
}