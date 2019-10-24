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