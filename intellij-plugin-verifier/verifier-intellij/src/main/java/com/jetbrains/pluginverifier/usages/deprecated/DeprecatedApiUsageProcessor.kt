package com.jetbrains.pluginverifier.usages.deprecated

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class DeprecatedApiUsageProcessor(private val deprecatedApiRegistrar: DeprecatedApiRegistrar) : ApiUsageProcessor {
  override fun processApiUsage(classFileMember: ClassFileMember, usageLocation: Location, context: VerificationContext) {
    val deprecationInfo = classFileMember.getDeprecationInfo() ?: return
    when (classFileMember) {
      is ClassFile -> {
        deprecatedApiRegistrar.registerDeprecatedUsage(
            DeprecatedClassUsage(classFileMember.location, usageLocation, deprecationInfo)
        )
      }
      is Method -> {
        deprecatedApiRegistrar.registerDeprecatedUsage(
            DeprecatedMethodUsage(classFileMember.location, usageLocation, deprecationInfo)
        )
      }
      is Field -> {
        deprecatedApiRegistrar.registerDeprecatedUsage(
            DeprecatedFieldUsage(classFileMember.location, usageLocation, deprecationInfo)
        )
      }
    }
  }
}