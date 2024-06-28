/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.deprecated

import com.jetbrains.pluginverifier.verifiers.findAnnotation
import com.jetbrains.pluginverifier.verifiers.getAnnotationValue
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import org.objectweb.asm.tree.AnnotationNode

internal const val JETBRAINS_SCHEDULED_FOR_REMOVAL_ANNOTATION_NAME = "org/jetbrains/annotations/ApiStatus\$ScheduledForRemoval"
internal const val JAVA_DEPRECATED_ANNOTATION_NAME = "java/lang/Deprecated"
internal const val KOTLIN_DEPRECATED_ANNOTATION_NAME = "kotlin/Deprecated"

/**
 * Extracts [DeprecationInfo] for the following cases:
 *
 * 1) @deprecated JavaDoc
 * 2) @Deprecated (Java 8)
 * 3) @Deprecated (Java 9) and `forRemoval = true | false`
 * 4) @ScheduledForRemoval, `inVersion` is specified or not
 * 5) @Deprecated (Kotlin) for non-hidden deprecation levels
 */
val ClassFileMember.deprecationInfo: DeprecationInfo?
  get() {
    val annotations = annotations
    val scheduledForRemoval = annotations.findAnnotation(JETBRAINS_SCHEDULED_FOR_REMOVAL_ANNOTATION_NAME)
    if (scheduledForRemoval != null) {
      val inVersion = scheduledForRemoval.getAnnotationValue("inVersion") as? String
      return DeprecationInfo(true, inVersion)
    }

    val deprecated = annotations.findAnnotation(JAVA_DEPRECATED_ANNOTATION_NAME)
    if (deprecated != null) {
      val forRemoval = deprecated.getAnnotationValue("forRemoval") as? Boolean ?: false
      return DeprecationInfo(forRemoval, null)
    }

    val kotlinDeprecated = annotations.findAnnotation(KOTLIN_DEPRECATED_ANNOTATION_NAME)
    if (kotlinDeprecated != null) {
      val deprecationLevel = kotlinDeprecated.getEnumValue<DeprecationLevel>("level")
      if (deprecationLevel == DeprecationLevel.HIDDEN) {
        return null
      }
    }

    return if (isDeprecated) {
      DeprecationInfo(false, null)
    } else {
      null
    }
  }

private inline fun <reified T : Enum<T>> AnnotationNode.getEnumValue(name: String): T? {
  val annValue = getAnnotationValue(name)
  // see org.objectweb.asm.tree.AnnotationNode.values semantics
  return if (annValue is Array<*> && annValue.size == 2)  {
    enumValueOrNull<T>(annValue[1] as String)
  } else {
    null
  }
}

private inline fun <reified T : Enum<T>> enumValueOrNull(name: String): T? {
  return try {
    enumValueOf<T>(name)
  } catch (e: IllegalArgumentException) {
    null
  }
}
