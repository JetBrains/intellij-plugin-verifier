/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.internal

import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

class InternalApiUsageProcessor(pluginVerificationContext: PluginVerificationContext) : BaseInternalApiUsageProcessor(
  AnnotatedInternalApiUsageRegistrar(pluginVerificationContext)
) {

  override fun isInternal(
    resolvedMember: ClassFileMember,
    context: VerificationContext,
    usageLocation: Location
  ): Boolean = resolvedMember.isInternalApi(context.classResolver)
    && resolvedMember.containingClassFile.classFileOrigin != usageLocation.containingClass.classFileOrigin
}