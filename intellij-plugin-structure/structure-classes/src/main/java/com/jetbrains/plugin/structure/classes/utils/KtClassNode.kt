package com.jetbrains.plugin.structure.classes.utils

import kotlinx.metadata.Visibility
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.visibility
import org.objectweb.asm.tree.ClassNode

class KtClassNode(private val classNode: ClassNode, private val metadata: KotlinClassMetadata) {
  val isInternal: Boolean
    get() = metadata is KotlinClassMetadata.Class && metadata.kmClass.visibility == Visibility.INTERNAL

  val name: String
    get() = classNode.name
}