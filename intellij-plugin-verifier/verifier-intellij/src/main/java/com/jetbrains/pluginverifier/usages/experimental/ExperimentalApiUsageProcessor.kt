package com.jetbrains.pluginverifier.usages.experimental

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class ExperimentalApiUsageProcessor(private val experimentalApiRegistrar: ExperimentalApiRegistrar) : ApiUsageProcessor {
  override fun processApiUsage(classFileMember: ClassFileMember, usageLocation: Location, context: VerificationContext) {
    if (classFileMember.isExperimentalApi(context.classResolver)
        && classFileMember.containingClassFile.classFileOrigin != usageLocation.containingClass.classFileOrigin
    ) {
      when (classFileMember) {
        is ClassFile -> {
          experimentalApiRegistrar.registerExperimentalApiUsage(
              ExperimentalClassUsage(classFileMember.location, usageLocation)
          )
        }
        is Method -> {
          experimentalApiRegistrar.registerExperimentalApiUsage(
              ExperimentalMethodUsage(classFileMember.location, usageLocation)
          )
        }
        is Field -> {
          experimentalApiRegistrar.registerExperimentalApiUsage(
              ExperimentalFieldUsage(classFileMember.location, usageLocation)
          )
        }
      }
    }
  }
}