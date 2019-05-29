package com.jetbrains.pluginverifier.usages.deprecated

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class DeprecatedApiUsageProcessor : ApiUsageProcessor {
  override fun processApiUsage(classFileMember: ClassFileMember, usageLocation: Location, context: VerificationContext) {
    if (context is DeprecatedApiRegistrar) {
      val deprecationInfo = classFileMember.getDeprecationInfo() ?: return
      when (classFileMember) {
        is ClassFile -> {
          context.registerDeprecatedUsage(
              DeprecatedClassUsage(classFileMember.location, usageLocation, deprecationInfo)
          )
        }
        is Method -> {
          context.registerDeprecatedUsage(
              DeprecatedMethodUsage(classFileMember.location, usageLocation, deprecationInfo)
          )
        }
        is Field -> {
          context.registerDeprecatedUsage(
              DeprecatedFieldUsage(classFileMember.location, usageLocation, deprecationInfo)
          )
        }
      }
    }
  }
}