package com.jetbrains.pluginverifier.usages

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedClassUsage
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedFieldUsage
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedMethodUsage
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class DeprecatedApiUsageProcessor : ApiUsageProcessor {
  override fun processApiUsage(classFileMember: ClassFileMember, usageLocation: Location, context: VerificationContext) {
    if (context is DeprecatedApiRegistrar) {
      when (classFileMember) {
        is ClassFile -> {
          val classDeprecated = classFileMember.getDeprecationInfo()
          if (classDeprecated != null) {
            context.registerDeprecatedUsage(
                DeprecatedClassUsage(classFileMember.location, usageLocation, classDeprecated)
            )
          }
        }
        is Method -> {
          val methodDeprecated = classFileMember.getDeprecationInfo()
          if (methodDeprecated != null) {
            context.registerDeprecatedUsage(
                DeprecatedMethodUsage(classFileMember.location, usageLocation, methodDeprecated)
            )
          }
        }
        is Field -> {
          val fieldDeprecated = classFileMember.getDeprecationInfo()
          if (fieldDeprecated != null) {
            context.registerDeprecatedUsage(
                DeprecatedFieldUsage(classFileMember.location, usageLocation, fieldDeprecated)
            )
          }
        }
      }
    }
  }
}