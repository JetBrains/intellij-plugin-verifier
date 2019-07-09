package com.jetbrains.pluginverifier.usages.discouraging

import com.jetbrains.plugin.structure.classes.resolvers.JdkClassFileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.isOriginOfType
import com.jetbrains.plugin.structure.ide.classes.IdeClassFileOrigin
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiRegistrar
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.isDiscouragingJdkClass

class DiscouragingClassUsageProcessor : ApiUsageProcessor {
  override fun processApiUsage(classFileMember: ClassFileMember, usageLocation: Location, context: VerificationContext) {
    if (context is DeprecatedApiRegistrar && classFileMember is ClassFile && classFileMember.isDiscouragingJdkClass()) {
      val classFileOrigin = classFileMember.classFileOrigin
      if (classFileOrigin.isOriginOfType<IdeClassFileOrigin>() || classFileOrigin.isOriginOfType<JdkClassFileOrigin>()) {
        context.registerDeprecatedUsage(
            DiscouragingJdkClassUsage(classFileMember.location, usageLocation, classFileOrigin)
        )
      }
    }
  }
}