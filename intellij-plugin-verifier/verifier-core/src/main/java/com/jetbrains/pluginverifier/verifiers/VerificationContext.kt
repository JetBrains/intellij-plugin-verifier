/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

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