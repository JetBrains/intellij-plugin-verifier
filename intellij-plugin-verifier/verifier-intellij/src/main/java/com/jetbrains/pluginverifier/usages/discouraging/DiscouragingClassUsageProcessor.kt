package com.jetbrains.pluginverifier.usages.discouraging

import com.jetbrains.plugin.structure.classes.resolvers.JdkClassFileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.isOriginOfType
import com.jetbrains.plugin.structure.ide.classes.IdeClassFileOrigin
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.reference.SymbolicReference
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiRegistrar
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.isDiscouragingJdkClass

class DiscouragingClassUsageProcessor(private val deprecatedApiRegistrar: DeprecatedApiRegistrar) : ApiUsageProcessor {
  override fun processApiUsage(
      apiReference: SymbolicReference,
      resolvedMember: ClassFileMember,
      usageLocation: Location,
      context: VerificationContext
  ) {
    if (resolvedMember is ClassFile && resolvedMember.isDiscouragingJdkClass()) {
      val classFileOrigin = resolvedMember.classFileOrigin
      if (classFileOrigin.isOriginOfType<IdeClassFileOrigin>() || classFileOrigin.isOriginOfType<JdkClassFileOrigin>()) {
        deprecatedApiRegistrar.registerDeprecatedUsage(
            DiscouragingJdkClassUsage(resolvedMember.location, usageLocation, classFileOrigin)
        )
      }
    }
  }
}