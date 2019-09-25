package com.jetbrains.pluginverifier.usages.discouraging

import com.jetbrains.plugin.structure.classes.resolvers.JdkFileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.isOriginOfType
import com.jetbrains.plugin.structure.ide.classes.IdeFileOrigin
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiRegistrar
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.isDiscouragingJdkClass

class DiscouragingClassUsageProcessor(private val deprecatedApiRegistrar: DeprecatedApiRegistrar) : ApiUsageProcessor {
  override fun processClassReference(
      classReference: ClassReference,
      resolvedClass: ClassFile,
      context: VerificationContext,
      referrer: ClassFileMember
  ) {
    if (resolvedClass.isDiscouragingJdkClass()) {
      val classFileOrigin = resolvedClass.classFileOrigin
      if (classFileOrigin.isOriginOfType<IdeFileOrigin>() || classFileOrigin.isOriginOfType<JdkFileOrigin>()) {
        deprecatedApiRegistrar.registerDeprecatedUsage(
            DiscouragingJdkClassUsage(resolvedClass.location, referrer.location, classFileOrigin)
        )
      }
    }
  }
}