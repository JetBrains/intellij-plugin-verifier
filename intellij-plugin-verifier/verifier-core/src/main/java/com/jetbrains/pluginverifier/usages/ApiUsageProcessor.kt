package com.jetbrains.pluginverifier.usages

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

interface ApiUsageProcessor {
  fun processApiUsage(classFileMember: ClassFileMember, usageLocation: Location, context: VerificationContext)
}