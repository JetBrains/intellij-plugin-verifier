/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.experimental

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.usages.ApiUsage
import com.jetbrains.pluginverifier.usages.annotation.AnnotationResolver
import com.jetbrains.pluginverifier.usages.annotation.isMemberEffectivelyAnnotatedWith
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

/**
 * Usage of `@org.jetbrains.annotations.ApiStatus.Experimental` API.
 */
abstract class ExperimentalApiUsage : ApiUsage()

fun ClassFileMember.isExperimentalApi(classResolver: Resolver, usageLocation: Location): Boolean =
  isMemberEffectivelyAnnotatedWith(experimentalApiStatusResolver, classResolver, location)

private val experimentalApiStatusResolver = AnnotationResolver("org/jetbrains/annotations/ApiStatus\$Experimental")