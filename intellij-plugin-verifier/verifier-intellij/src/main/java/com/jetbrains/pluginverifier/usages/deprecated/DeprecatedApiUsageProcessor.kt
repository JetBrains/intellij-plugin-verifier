package com.jetbrains.pluginverifier.usages.deprecated

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

class DeprecatedApiUsageProcessor(private val deprecatedApiRegistrar: DeprecatedApiRegistrar) : ApiUsageProcessor {
  override fun processApiUsage(
      apiReference: SymbolicReference,
      resolvedMember: ClassFileMember,
      usageLocation: Location,
      context: VerificationContext
  ) {
    val deprecationInfo = resolvedMember.deprecationInfo ?: return
    when (resolvedMember) {
      is ClassFile -> {
        deprecatedApiRegistrar.registerDeprecatedUsage(
            DeprecatedClassUsage(apiReference as ClassReference, resolvedMember.location, usageLocation, deprecationInfo)
        )
      }
      is Method -> {
        deprecatedApiRegistrar.registerDeprecatedUsage(
            DeprecatedMethodUsage(apiReference as MethodReference, resolvedMember.location, usageLocation, deprecationInfo)
        )
      }
      is Field -> {
        deprecatedApiRegistrar.registerDeprecatedUsage(
            DeprecatedFieldUsage(apiReference as FieldReference, resolvedMember.location, usageLocation, deprecationInfo)
        )
      }
    }
  }
}