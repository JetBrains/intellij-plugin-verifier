package com.jetbrains.pluginverifier.usages.experimental

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

class ExperimentalApiUsageProcessor(private val experimentalApiRegistrar: ExperimentalApiRegistrar) : ApiUsageProcessor {
  override fun processApiUsage(
      apiReference: SymbolicReference,
      resolvedMember: ClassFileMember,
      usageLocation: Location,
      context: VerificationContext
  ) {
    if (resolvedMember.isExperimentalApi(context.classResolver)
        && resolvedMember.containingClassFile.classFileOrigin != usageLocation.containingClass.classFileOrigin
    ) {
      when (resolvedMember) {
        is ClassFile -> {
          experimentalApiRegistrar.registerExperimentalApiUsage(
              ExperimentalClassUsage(apiReference as ClassReference, resolvedMember.location, usageLocation)
          )
        }
        is Method -> {
          experimentalApiRegistrar.registerExperimentalApiUsage(
              ExperimentalMethodUsage(apiReference as MethodReference, resolvedMember.location, usageLocation)
          )
        }
        is Field -> {
          experimentalApiRegistrar.registerExperimentalApiUsage(
              ExperimentalFieldUsage(apiReference as FieldReference, resolvedMember.location, usageLocation)
          )
        }
      }
    }
  }
}