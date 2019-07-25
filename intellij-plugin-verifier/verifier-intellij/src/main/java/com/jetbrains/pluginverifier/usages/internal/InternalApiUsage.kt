package com.jetbrains.pluginverifier.usages.internal

import com.jetbrains.pluginverifier.usages.ApiUsage
import com.jetbrains.pluginverifier.usages.util.isMemberEffectivelyAnnotatedWith
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

/**
 * Usage of `@org.jetbrains.annotations.ApiStatus.Internal` API.
 */
abstract class InternalApiUsage : ApiUsage()

fun ClassFileMember.isInternalApi(context: VerificationContext): Boolean =
    isMemberEffectivelyAnnotatedWith("org/jetbrains/annotations/ApiStatus\$Internal", context.classResolver)