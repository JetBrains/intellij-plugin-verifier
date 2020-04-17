/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.results.location

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.results.presentation.*
import java.util.*

sealed class Location {
  abstract val presentableLocation: String

  abstract val elementType: ElementType

  abstract val containingClass: ClassLocation

  final override fun toString() = presentableLocation
}

data class ClassLocation(
  val className: String,
  val signature: String?,
  val modifiers: Modifiers,
  val classFileOrigin: FileOrigin
) : Location() {

  val packageName
    get() = className.substringBeforeLast('/', "")

  override fun equals(other: Any?) = other is ClassLocation && className == other.className

  override fun hashCode() = className.hashCode()

  override val presentableLocation
    get() = formatClassLocation(ClassOption.FULL_NAME, ClassGenericsSignatureOption.WITH_GENERICS)

  override val elementType: ElementType
    get() = with(modifiers) {
      when {
        contains(Modifiers.Modifier.INTERFACE) -> ElementType.INTERFACE
        contains(Modifiers.Modifier.ENUM) -> ElementType.ENUM
        contains(Modifiers.Modifier.ANNOTATION) -> ElementType.ANNOTATION
        else -> ElementType.CLASS
      }
    }

  override val containingClass
    get() = this
}

data class FieldLocation(
  val hostClass: ClassLocation,
  val fieldName: String,
  val fieldDescriptor: String,
  val signature: String?,
  val modifiers: Modifiers
) : Location() {

  override fun equals(other: Any?) = other is FieldLocation
    && hostClass == other.hostClass
    && fieldName == other.fieldName
    && fieldDescriptor == other.fieldDescriptor

  override fun hashCode() = Objects.hash(hostClass, fieldName, fieldDescriptor)

  override val presentableLocation
    get() = formatFieldLocation(HostClassOption.FULL_HOST_WITH_SIGNATURE, FieldTypeOption.SIMPLE_TYPE)

  override val elementType: ElementType
    get() = ElementType.FIELD

  override val containingClass
    get() = hostClass
}

data class MethodLocation(
  val hostClass: ClassLocation,
  val methodName: String,
  val methodDescriptor: String,
  val parameterNames: List<String>,
  val signature: String?,
  val modifiers: Modifiers
) : Location() {

  override fun equals(other: Any?) = other is MethodLocation
    && hostClass == other.hostClass
    && methodName == other.methodName
    && methodDescriptor == other.methodDescriptor

  override fun hashCode() = Objects.hash(hostClass, methodName, methodDescriptor)

  override val presentableLocation
    get() = formatMethodLocation(HostClassOption.FULL_HOST_WITH_SIGNATURE, MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME, MethodReturnTypeOption.SIMPLE_RETURN_TYPE_CLASS_NAME, MethodParameterNameOption.WITH_PARAM_NAMES_IF_AVAILABLE)

  override val elementType: ElementType
    get() = if (methodName == "<init>") {
      ElementType.CONSTRUCTOR
    } else {
      ElementType.METHOD
    }

  override val containingClass
    get() = hostClass
}