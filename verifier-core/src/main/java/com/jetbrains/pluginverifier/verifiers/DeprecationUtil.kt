package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.results.deprecated.DeprecationInfo
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

private const val SCHEDULED_FOR_REMOVAL_CLASS = "org/jetbrains/annotations/ApiStatus\$ScheduledForRemoval"

private const val SCHEDULED_FOR_REMOVAL_IN_VERSION_KEY = "inVersion"

private const val DEPRECATED_CLASS = "java/lang/Deprecated"

private const val DEPRECATED_FOR_REMOVAL_KEY = "forRemoval"

fun ClassNode.getDeprecationInfo(): DeprecationInfo? {
  return findDeprecationInfo(getInvisibleAnnotations(), isDeprecated())
}

fun MethodNode.getDeprecationInfo(): DeprecationInfo? {
  return findDeprecationInfo(getInvisibleAnnotations(), isDeprecated())
}

fun FieldNode.getDeprecationInfo(): DeprecationInfo? {
  return findDeprecationInfo(getInvisibleAnnotations(), isDeprecated())
}

/**
 * Extracts [DeprecationInfo] for the following cases:
 *
 * 1) @deprecated JavaDoc
 * 2) @Deprecated (Java 8)
 * 3) @Deprecated (Java 9) and `forRemoval = true | false`
 * 4) @ScheduledForRemoval, `inVersion` is specified or not
 */
private fun findDeprecationInfo(annotations: List<AnnotationNode>?, isDeprecatedFlag: Boolean): DeprecationInfo? {
  if (annotations != null) {
    //Firstly try to find [SCHEDULED_FOR_REMOVAL_CLASS] annotation.
    val scheduledForRemoval = annotations.findAnnotation(SCHEDULED_FOR_REMOVAL_CLASS)
    if (scheduledForRemoval != null) {
      val inVersion = scheduledForRemoval.getAnnotationValue(SCHEDULED_FOR_REMOVAL_IN_VERSION_KEY) as? String
      return DeprecationInfo(true, inVersion)
    }

    /**
     * Secondly try to find [DEPRECATED_CLASS] annotation.
     */
    val deprecated = annotations.findAnnotation(DEPRECATED_CLASS)
    if (deprecated != null) {
      val forRemoval = deprecated.getAnnotationValue(DEPRECATED_FOR_REMOVAL_KEY) as? Boolean ?: false
      return DeprecationInfo(forRemoval, null)
    }
  }

  return if (isDeprecatedFlag) {
    DeprecationInfo(false, null)
  } else {
    null
  }
}
