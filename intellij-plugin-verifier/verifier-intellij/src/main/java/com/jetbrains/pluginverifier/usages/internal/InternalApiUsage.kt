/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.internal

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.usages.ApiUsage
import com.jetbrains.pluginverifier.usages.util.isMemberEffectivelyAnnotatedWith
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

/**
 * Usage of `@org.jetbrains.annotations.ApiStatus.Internal` API.
 */
abstract class InternalApiUsage : ApiUsage()

fun ClassFileMember.isInternalApi(resolver: Resolver): Boolean =
  isMemberEffectivelyAnnotatedWith("org/jetbrains/annotations/ApiStatus\$Internal", resolver)