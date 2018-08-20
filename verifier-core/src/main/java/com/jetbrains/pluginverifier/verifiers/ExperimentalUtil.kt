package com.jetbrains.pluginverifier.verifiers

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

private const val EXPERIMENTAL_CLASS = "org/jetbrains/annotations/ApiStatus\$Experimental"

fun ClassNode.isExperimentalApi() =
    getInvisibleAnnotations()?.findAnnotation(EXPERIMENTAL_CLASS) != null

fun MethodNode.isExperimentalApi() =
    getInvisibleAnnotations()?.findAnnotation(EXPERIMENTAL_CLASS) != null

fun FieldNode.isExperimentalApi() =
    getInvisibleAnnotations()?.findAnnotation(EXPERIMENTAL_CLASS) != null