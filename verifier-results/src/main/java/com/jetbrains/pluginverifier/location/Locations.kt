package com.jetbrains.pluginverifier.location

import com.google.gson.annotations.SerializedName
import com.jetbrains.pluginverifier.utils.MessageUtils
import org.objectweb.asm.tree.MethodNode

/**
 * @author Sergey Patrikeev
 */
interface ProblemLocation {

  companion object {
    fun fromClass(className: String) = ClassLocation(className)

    fun fromMethod(hostClass: String, method: MethodNode) = fromMethod(hostClass, method.name, method.desc)

    fun fromMethod(hostClass: String, methodName: String, methodDescriptor: String) = MethodLocation(hostClass, methodName, methodDescriptor)

    fun fromField(hostClass: String, fieldName: String, fieldDescriptor: String) = FieldLocation(hostClass, fieldName, fieldDescriptor)
  }
}

data class MethodLocation(@SerializedName("host") val hostClass: String,
                          @SerializedName("name") val methodName: String,
                          @SerializedName("descriptor") val methodDescriptor: String) : ProblemLocation {
  override fun toString(): String = MessageUtils.convertMethod(methodName, methodDescriptor, hostClass)
}


data class FieldLocation(@SerializedName("host") val hostClass: String,
                         @SerializedName("name") val fieldName: String,
                         @SerializedName("descriptor") val fieldDescriptor: String) : ProblemLocation {
  override fun toString(): String = MessageUtils.convertField(fieldName, fieldDescriptor, hostClass)
}

data class ClassLocation(@SerializedName("class") val className: String) : ProblemLocation {
  override fun toString(): String = MessageUtils.convertClass(className)
}