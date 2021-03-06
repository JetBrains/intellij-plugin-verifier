/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.results.reference

import com.jetbrains.pluginverifier.results.presentation.*
import java.util.*

sealed class SymbolicReference {
  abstract val presentableLocation: String

  final override fun toString() = presentableLocation
}

data class ClassReference(val className: String) : SymbolicReference() {
  override fun equals(other: Any?) = other is ClassReference && className == other.className

  override fun hashCode() = className.hashCode()

  override val presentableLocation
    get() = formatClassReference(ClassOption.FULL_NAME)
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

  override val presentableLocation
    get() = formatMethodReference(HostClassOption.FULL_HOST_NAME, MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME, MethodReturnTypeOption.SIMPLE_RETURN_TYPE_CLASS_NAME)

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

  override val presentableLocation
    get() = formatFieldReference(HostClassOption.FULL_HOST_NAME, FieldTypeOption.SIMPLE_TYPE)

}