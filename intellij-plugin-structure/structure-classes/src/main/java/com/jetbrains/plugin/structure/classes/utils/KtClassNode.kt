package com.jetbrains.plugin.structure.classes.utils

import kotlinx.metadata.ClassKind
import kotlinx.metadata.KmClass
import kotlinx.metadata.Visibility
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.signature
import kotlinx.metadata.kind
import kotlinx.metadata.visibility
import org.objectweb.asm.tree.ClassNode

class KtClassNode(private val classNode: ClassNode, private val metadata: KotlinClassMetadata.Class) {

  val name: String
    get() = classNode.name

  val isInternal: Boolean
    get() = cls.visibility == Visibility.INTERNAL

  val isEnumClass: Boolean
    get() = cls.kind == ClassKind.ENUM_CLASS

  fun isInternalField(fieldName: String): Boolean {
    return cls.properties.any { it.name == fieldName && it.visibility == Visibility.INTERNAL }
  }

  fun isInternalFunction(functionName: String, jvmDescriptor: String): Boolean {
    return cls.functions.any { it.name == functionName
      && it.signature?.descriptor == jvmDescriptor
      && it.visibility == Visibility.INTERNAL }
  }

  private val cls: KmClass
    get() = metadata.kmClass
}