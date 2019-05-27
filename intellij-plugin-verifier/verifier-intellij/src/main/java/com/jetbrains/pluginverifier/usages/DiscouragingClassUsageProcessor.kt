package com.jetbrains.pluginverifier.usages

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.usages.deprecated.DiscouragingJdkClassUsage
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.IntelliJClassFileOrigin
import com.jetbrains.pluginverifier.verifiers.resolution.isDiscouragingJdkClass

class DiscouragingClassUsageProcessor : ApiUsageProcessor {
  override fun processApiUsage(classFileMember: ClassFileMember, usageLocation: Location, context: VerificationContext) {
    if (context is DeprecatedApiRegistrar && classFileMember is ClassFile && classFileMember.isDiscouragingJdkClass()) {
      val classOrigin = classFileMember.classFileOrigin
      if (classOrigin is IntelliJClassFileOrigin.IdeClass || classOrigin is IntelliJClassFileOrigin.JdkClass) {
        val isClassProvidedByIde = classOrigin is IntelliJClassFileOrigin.IdeClass
        context.registerDeprecatedUsage(
            DiscouragingJdkClassUsage(classFileMember.location, usageLocation, isClassProvidedByIde)
        )
      }
    }
  }
}