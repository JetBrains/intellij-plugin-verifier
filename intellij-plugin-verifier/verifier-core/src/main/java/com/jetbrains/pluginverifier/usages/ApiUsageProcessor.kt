package com.jetbrains.pluginverifier.usages

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.reference.SymbolicReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

interface ApiUsageProcessor {
  fun processApiUsage(
      apiReference: SymbolicReference,
      resolvedMember: ClassFileMember,
      usageLocation: Location,
      context: VerificationContext
  )
}