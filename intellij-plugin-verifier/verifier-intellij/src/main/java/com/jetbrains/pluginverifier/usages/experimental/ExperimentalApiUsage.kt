package com.jetbrains.pluginverifier.usages.experimental

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.usages.ApiUsage
import com.jetbrains.pluginverifier.usages.util.MemberAnnotation
import com.jetbrains.pluginverifier.usages.util.findEffectiveMemberAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

/**
 * Usage of `@org.jetbrains.annotations.ApiStatus.Experimental` API.
 */
abstract class ExperimentalApiUsage : ApiUsage()

fun ClassFileMember.isExperimentalApi(resolver: Resolver): Boolean =
    findEffectiveExperimentalAnnotation(resolver) != null

fun ClassFileMember.findEffectiveExperimentalAnnotation(resolver: Resolver): MemberAnnotation? =
    findEffectiveMemberAnnotation("org/jetbrains/annotations/ApiStatus\$Experimental", resolver)