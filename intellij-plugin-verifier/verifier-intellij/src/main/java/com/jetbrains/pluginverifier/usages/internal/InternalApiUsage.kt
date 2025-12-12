/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.internal

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.usages.ApiUsage
import com.jetbrains.pluginverifier.usages.annotation.AnnotationResolver
import com.jetbrains.pluginverifier.usages.annotation.isMemberEffectivelyAnnotatedWith
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

/**
 * Usage of [InternalApiConstants.INTERNAL_API_ANNOTATION] or [InternalApiConstants.INTELLIJ_INTERNAL_API_ANNOTATION] API
 */
abstract class InternalApiUsage : ApiUsage()

fun ClassFileMember.isInternalApi(resolver: Resolver, location: Location): Boolean =
  !isIgnoredUsage(this, location)
    && listOf(internalApiStatusResolver, intellijInternalApiResolver)
    .any { isMemberEffectivelyAnnotatedWith(it, resolver, location) }

private fun isIgnoredUsage(resolvedMember: ClassFileMember, usageLocation: Location): Boolean
  = ignoredAPIs.any { predicate -> predicate(resolvedMember, usageLocation) }

typealias IgnoredUsagePredicate = (ClassFileMember, Location) -> Boolean

private val ignoredAPIs: List<IgnoredUsagePredicate> = listOf(
  { member, location -> (member.containingClassFile).name.endsWith("\$DefaultImpls")}
)

private val internalApiStatusResolver = AnnotationResolver("org/jetbrains/annotations/ApiStatus\$Internal")
private val intellijInternalApiResolver = AnnotationResolver("com/intellij/openapi/util/IntellijInternalApi")

