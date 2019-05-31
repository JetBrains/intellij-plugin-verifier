package com.jetbrains.pluginverifier.usages.internal

import com.jetbrains.pluginverifier.verifiers.findAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

fun ClassFileMember.isInternalApi() =
    runtimeInvisibleAnnotations.findAnnotation("org/jetbrains/annotations/ApiStatus\$Internal") != null