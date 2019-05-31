package com.jetbrains.pluginverifier.usages.nonExtendable

import com.jetbrains.pluginverifier.verifiers.findAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

fun ClassFileMember.isNonExtendable(): Boolean =
    runtimeInvisibleAnnotations.findAnnotation("org/jetbrains/annotations/ApiStatus\$NonExtendable") != null