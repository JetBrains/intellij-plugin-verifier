package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class OverrideOnlyMethodUsageProcessor : ApiUsageProcessor {
  override fun processApiUsage(classFileMember: ClassFileMember, usageLocation: Location, context: VerificationContext) {
    if (context is OverrideOnlyRegistrar && classFileMember is Method && classFileMember.isOverrideOnlyMethod()) {
      context.registerOverrideOnlyMethodUsage(OverrideOnlyMethodUsage(classFileMember.location, usageLocation))
    }
  }
}