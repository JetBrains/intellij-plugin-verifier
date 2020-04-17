/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

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
import com.jetbrains.pluginverifier.verifiers.resolution.ClassUsageType
import com.jetbrains.pluginverifier.verifiers.resolution.isDiscouragingJdkClass

class DiscouragingClassUsageProcessor(private val deprecatedApiRegistrar: DeprecatedApiRegistrar) : ApiUsageProcessor {
  override fun processClassReference(
    classReference: ClassReference,
    resolvedClass: ClassFile,
    context: VerificationContext,
    referrer: ClassFileMember,
    classUsageType: ClassUsageType
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