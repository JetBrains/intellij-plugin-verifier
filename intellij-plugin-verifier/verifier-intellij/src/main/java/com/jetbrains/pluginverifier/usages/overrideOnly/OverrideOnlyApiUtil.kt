package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.pluginverifier.verifiers.findAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.Method

private const val overrideOnlyAnnotationName = "org/jetbrains/annotations/ApiStatus\$OverrideOnly"

fun Method.isOverrideOnlyMethod(): Boolean =
    runtimeInvisibleAnnotations.findAnnotation(overrideOnlyAnnotationName) != null
        || containingClassFile.runtimeInvisibleAnnotations.findAnnotation(overrideOnlyAnnotationName) != null