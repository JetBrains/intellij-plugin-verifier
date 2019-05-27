package com.jetbrains.pluginverifier.usages

import com.jetbrains.pluginverifier.verifiers.findAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.Method

private const val EXPERIMENTAL_CLASS = "org/jetbrains/annotations/ApiStatus\$Experimental"

fun ClassFile.isExperimentalApi() =
    invisibleAnnotations.findAnnotation(EXPERIMENTAL_CLASS) != null

fun Method.isExperimentalApi() =
    invisibleAnnotations.findAnnotation(EXPERIMENTAL_CLASS) != null

fun Field.isExperimentalApi() =
    invisibleAnnotations.findAnnotation(EXPERIMENTAL_CLASS) != null