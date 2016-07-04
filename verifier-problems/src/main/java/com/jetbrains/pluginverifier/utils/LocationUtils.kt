package com.jetbrains.pluginverifier.utils

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

/**
 * @author Sergey Patrikeev
 */
object LocationUtils {

  fun getMethodLocation(classNode: ClassNode, methodNode: MethodNode): String {
    return getMethodLocation(classNode.name, methodNode)
  }

  fun getMethodLocation(ownerClassName: String, methodName: String, methodDesc: String): String {
    //NotNull checks are performed automatically
    return ownerClassName + '#' + methodName + methodDesc
  }

  fun getMethodLocation(owner: ClassNode, methodName: String, methodDesc: String): String {
    return getMethodLocation(owner.name, methodName, methodDesc)
  }

  fun getMethodLocation(ownerClassName: String, methodNode: MethodNode): String {
    return getMethodLocation(ownerClassName, methodNode.name, methodNode.desc)
  }

  fun getFieldLocation(owner: ClassNode, fieldName: String, fieldDescriptor: String): String {
    return getFieldLocation(owner.name, fieldName, fieldDescriptor)
  }

  fun getFieldLocation(ownerClassName: String, fieldName: String, fieldDescriptor: String): String {
    //NotNull checks are performed automatically
    return "$ownerClassName#$fieldName#$fieldDescriptor"
  }

  fun getFieldLocation(ownerClassName: String, fieldNode: FieldNode): String {
    return getFieldLocation(ownerClassName, fieldNode.name, fieldNode.desc)
  }
}
