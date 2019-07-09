package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.usages.ApiUsageProcessor
import com.jetbrains.pluginverifier.verifiers.packages.PackageFilter

interface VerificationContext {
  val classResolver: Resolver

  val externalClassesPackageFilter: PackageFilter

  val problemRegistrar: ProblemRegistrar

  val apiUsageProcessors: List<ApiUsageProcessor>
}