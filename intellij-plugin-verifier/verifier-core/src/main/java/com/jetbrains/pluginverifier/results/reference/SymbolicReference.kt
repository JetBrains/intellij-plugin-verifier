package com.jetbrains.pluginverifier.results.reference

import com.jetbrains.pluginverifier.results.presentation.*
import java.util.*

sealed class SymbolicReference

data class ClassReference(val className: String) : SymbolicReference() {
  override fun equals(other: Any?) = other is ClassReference && className == other.className

  override fun hashCode() = className.hashCode()

  override fun toString() = formatClassReference(ClassOption.FULL_NAME)
}

data class MethodReference(
    val hostClass: ClassReference,
    val methodName: String,
    val methodDescriptor: String
) : SymbolicReference() {

  constructor(hostClass: String, methodName: String, methodDescriptor: String)
      : this(ClassReference(hostClass), methodName, methodDescriptor)

  override fun equals(other: Any?) = other is MethodReference
      && hostClass == other.hostClass
      && methodName == other.methodName
      && methodDescriptor == other.methodDescriptor

  override fun hashCode() = Objects.hash(hostClass, methodName, methodDescriptor)

  override fun toString() = formatMethodReference(HostClassOption.FULL_HOST_NAME, MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME, MethodReturnTypeOption.SIMPLE_RETURN_TYPE_CLASS_NAME)

}

data class FieldReference(
    val hostClass: ClassReference,
    val fieldName: String,
    val fieldDescriptor: String
) : SymbolicReference() {

  constructor(hostClass: String, fieldName: String, fieldDescriptor: String)
      : this(ClassReference(hostClass), fieldName, fieldDescriptor)

  override fun equals(other: Any?) = other is FieldReference
      && fieldName == other.fieldName
      && fieldDescriptor == other.fieldDescriptor

  override fun hashCode() = Objects.hash(hostClass, fieldName, fieldDescriptor)

  override fun toString() = formatFieldReference(HostClassOption.FULL_HOST_NAME, FieldTypeOption.SIMPLE_TYPE)

}