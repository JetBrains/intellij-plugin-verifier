package com.jetbrains.pluginverifier.usages.experimental

import com.jetbrains.pluginverifier.verifiers.findAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

fun ClassFileMember.isExperimentalApi() =
    runtimeInvisibleAnnotations.findAnnotation("org/jetbrains/annotations/ApiStatus\$Experimental") != null