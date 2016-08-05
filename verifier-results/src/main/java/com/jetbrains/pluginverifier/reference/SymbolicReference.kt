package com.jetbrains.pluginverifier.reference

import com.google.gson.annotations.SerializedName
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

/**
 * @author Sergey Patrikeev
 */
interface SymbolicReference {
  fun presentableForm(): String

}

data class MethodReference(@SerializedName("host") val hostClass: String,
                           @SerializedName("name") val methodName: String,
                           @SerializedName("descriptor") val methodDescriptor: String) : SymbolicReference {
  override fun presentableForm(): String = "$hostClass#$methodName$methodDescriptor"

  companion object {
    fun from(hostClass: String, methodName: String, methodDescriptor: String): MethodReference = MethodReference(hostClass, methodName, methodDescriptor)

    fun from(hostClass: ClassNode, methodName: String, methodDescriptor: String): MethodReference = from(hostClass.name, methodName, methodDescriptor)

    fun from(hostClass: String, method: MethodNode): MethodReference = from(hostClass, method.name, method.desc)

    fun from(hostClass: ClassNode, method: MethodNode): MethodReference = from(hostClass.name, method)
  }
}


data class FieldReference(@SerializedName("host") val hostClass: String,
                          @SerializedName("name") val fieldName: String,
                          @SerializedName("descriptor") val fieldDescriptor: String) : SymbolicReference {
  override fun presentableForm(): String = "$hostClass#$fieldName"

  companion object {

    fun from(hostClass: ClassNode, field: FieldNode): FieldReference = from(hostClass.name, field)

    fun from(hostClass: String, field: FieldNode): FieldReference = from(hostClass, field.name, field.desc)

    fun from(hostClass: String, fieldName: String, fieldDescriptor: String): FieldReference = FieldReference(hostClass, fieldName, fieldDescriptor)
  }
}

data class ClassReference(@SerializedName("class") val className: String) : SymbolicReference {
  override fun presentableForm(): String = className
}

