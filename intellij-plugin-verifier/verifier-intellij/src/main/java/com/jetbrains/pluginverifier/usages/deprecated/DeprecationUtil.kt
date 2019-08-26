package com.jetbrains.pluginverifier.usages.deprecated

import com.jetbrains.pluginverifier.verifiers.findAnnotation
import com.jetbrains.pluginverifier.verifiers.getAnnotationValue
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

/**
 * Extracts [DeprecationInfo] for the following cases:
 *
 * 1) @deprecated JavaDoc
 * 2) @Deprecated (Java 8)
 * 3) @Deprecated (Java 9) and `forRemoval = true | false`
 * 4) @ScheduledForRemoval, `inVersion` is specified or not
 */
val ClassFileMember.deprecationInfo: DeprecationInfo?
  get() {
    val annotations = runtimeInvisibleAnnotations
    val scheduledForRemoval = annotations.findAnnotation("org/jetbrains/annotations/ApiStatus\$ScheduledForRemoval")
    if (scheduledForRemoval != null) {
      val inVersion = scheduledForRemoval.getAnnotationValue("inVersion") as? String
      return DeprecationInfo(true, inVersion)
    }

    val deprecated = annotations.findAnnotation("java/lang/Deprecated")
    if (deprecated != null) {
      val forRemoval = deprecated.getAnnotationValue("forRemoval") as? Boolean ?: false
      return DeprecationInfo(forRemoval, null)
    }

    return if (isDeprecated) {
      DeprecationInfo(false, null)
    } else {
      null
    }
  }
