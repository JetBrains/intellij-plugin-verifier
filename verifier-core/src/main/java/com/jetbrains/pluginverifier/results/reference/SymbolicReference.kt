package com.jetbrains.pluginverifier.results.reference

import com.jetbrains.pluginverifier.results.presentation.*

interface SymbolicReference {
  companion object {

    fun methodOf(hostClass: String, methodName: String, methodDescriptor: String): MethodReference = MethodReference(ClassReference(hostClass), methodName, methodDescriptor)

    fun fieldOf(hostClass: String, fieldName: String, fieldDescriptor: String): FieldReference = FieldReference(ClassReference(hostClass), fieldName, fieldDescriptor)

    fun classOf(className: String): ClassReference = ClassReference(className)
  }
}

data class MethodReference(val hostClass: ClassReference,
                           val methodName: String,
                           val methodDescriptor: String) : SymbolicReference {

  override fun toString(): String = formatMethodReference(HostClassOption.FULL_HOST_NAME, MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME, MethodReturnTypeOption.SIMPLE_RETURN_TYPE_CLASS_NAME)

}


data class FieldReference(val hostClass: ClassReference,
                          val fieldName: String,
                          val fieldDescriptor: String) : SymbolicReference {

  override fun toString(): String = formatFieldReference(HostClassOption.FULL_HOST_NAME, FieldTypeOption.SIMPLE_TYPE)

}

data class ClassReference(val className: String) : SymbolicReference {
  override fun toString(): String = formatClassReference(ClassOption.FULL_NAME)
}