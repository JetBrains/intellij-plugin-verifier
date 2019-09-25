package com.jetbrains.pluginverifier.verifiers.resolution

import org.objectweb.asm.tree.AnnotationNode

data class MethodParameter(val name: String, val annotations: List<AnnotationNode>)