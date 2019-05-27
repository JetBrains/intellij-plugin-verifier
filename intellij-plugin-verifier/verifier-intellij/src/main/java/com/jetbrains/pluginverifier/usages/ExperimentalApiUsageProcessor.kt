package com.jetbrains.pluginverifier.usages

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalClassUsage
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalFieldUsage
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalMethodUsage
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class ExperimentalApiUsageProcessor : ApiUsageProcessor {
  override fun processApiUsage(classFileMember: ClassFileMember, usageLocation: Location, context: VerificationContext) {
    if (context is ExperimentalApiRegistrar) {
      when (classFileMember) {
        is ClassFile -> {
          if (classFileMember.isExperimentalApi()) {
            context.registerExperimentalApiUsage(
                ExperimentalClassUsage(classFileMember.location, usageLocation)
            )
          }
        }
        is Method -> {
          if (classFileMember.isExperimentalApi()) {
            context.registerExperimentalApiUsage(
                ExperimentalMethodUsage(classFileMember.location, usageLocation)
            )
          }
        }
        is Field -> {
          if (classFileMember.isExperimentalApi()) {
            context.registerExperimentalApiUsage(
                ExperimentalFieldUsage(classFileMember.location, usageLocation)
            )
          }
        }
      }
    }
  }
}