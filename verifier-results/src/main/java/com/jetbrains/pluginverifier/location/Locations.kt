package com.jetbrains.pluginverifier.location

import com.google.gson.annotations.SerializedName
import org.objectweb.asm.tree.MethodNode

/**
 * @author Sergey Patrikeev
 */
interface ProblemLocation {
  fun presentableForm(): String

  companion object {
    fun fromClass(className: String) = ClassLocation(className)

    fun fromMethod(hostClass: String, method: MethodNode) = fromMethod(hostClass, method.name, method.desc)

    fun fromMethod(hostClass: String, methodName: String, methodDescriptor: String) = MethodLocation(hostClass, methodName, methodDescriptor)

    fun fromField(hostClass: String, fieldName: String) = FieldLocation(hostClass, fieldName)
  }
}

data class MethodLocation(@SerializedName("host") val hostClass: String,
                          @SerializedName("name") val methodName: String,
                          @SerializedName("descriptor") val methodDescriptor: String) : ProblemLocation {
  override fun presentableForm(): String = "$hostClass#$methodName$methodDescriptor"
}


data class FieldLocation(@SerializedName("host") val hostClass: String,
                         @SerializedName("name") val fieldName: String) : ProblemLocation {
  override fun presentableForm(): String = "$hostClass#$fieldName"
}

data class ClassLocation(@SerializedName("class") val className: String) : ProblemLocation {
  override fun presentableForm(): String = className
}