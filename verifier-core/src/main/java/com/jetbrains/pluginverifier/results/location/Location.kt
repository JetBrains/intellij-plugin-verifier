package com.jetbrains.pluginverifier.results.location

import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.results.presentation.*
import java.io.Serializable
import java.util.*

/**
 * Location in the Java bytecode of a programming element
 * such as [class][ClassLocation], [method][MethodLocation]
 * or [field][FieldLocation].
 */
sealed class Location : Serializable {
  abstract val elementType: ElementType
}

data class ClassLocation(
    val className: String,
    val signature: String,
    val modifiers: Modifiers
) : Location() {
  override fun equals(other: Any?) = other is ClassLocation && className == other.className

  override fun hashCode() = className.hashCode()

  override fun toString() = formatClassLocation(ClassOption.FULL_NAME, ClassGenericsSignatureOption.WITH_GENERICS)

  override val elementType: ElementType
    get() = with(modifiers) {
      when {
        contains(Modifiers.Modifier.INTERFACE) -> ElementType.INTERFACE
        contains(Modifiers.Modifier.ENUM) -> ElementType.ENUM
        contains(Modifiers.Modifier.ANNOTATION) -> ElementType.ANNOTATION
        else -> ElementType.CLASS
      }
    }
}

data class FieldLocation(
    val hostClass: ClassLocation,
    val fieldName: String,
    val fieldDescriptor: String,
    val signature: String,
    val modifiers: Modifiers
) : Location() {

  override fun equals(other: Any?) = other is FieldLocation
      && hostClass == other.hostClass
      && fieldName == other.fieldName
      && fieldDescriptor == other.fieldDescriptor

  override fun hashCode() = Objects.hash(hostClass, fieldName, fieldDescriptor)

  override fun toString() = formatFieldLocation(HostClassOption.FULL_HOST_WITH_SIGNATURE, FieldTypeOption.SIMPLE_TYPE)

  override val elementType: ElementType
    get() = ElementType.FIELD
}

data class MethodLocation(
    val hostClass: ClassLocation,
    val methodName: String,
    val methodDescriptor: String,
    val parameterNames: List<String>,
    val signature: String,
    val modifiers: Modifiers
) : Location() {

  override fun equals(other: Any?) = other is MethodLocation
      && hostClass == other.hostClass
      && methodName == other.methodName
      && methodDescriptor == other.methodDescriptor

  override fun hashCode() = Objects.hash(hostClass, methodName, methodDescriptor)

  override fun toString() = formatMethodLocation(HostClassOption.FULL_HOST_WITH_SIGNATURE, MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME, MethodReturnTypeOption.SIMPLE_RETURN_TYPE_CLASS_NAME, MethodParameterNameOption.WITH_PARAM_NAMES_IF_AVAILABLE)

  override val elementType: ElementType
    get() = if (methodName == "<init>") {
      ElementType.CONSTRUCTOR
    } else {
      ElementType.METHOD
    }
}