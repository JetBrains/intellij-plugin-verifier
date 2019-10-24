package com.jetbrains.pluginverifier.results.location

import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.results.reference.SymbolicReference

fun Location.toReference(): SymbolicReference = when (this) {
  is ClassLocation -> toReference()
  is MethodLocation -> toReference()
  is FieldLocation -> toReference()
}

fun ClassLocation.toReference() = ClassReference(className)

fun MethodLocation.toReference(): MethodReference =
  MethodReference(hostClass.toReference(), methodName, methodDescriptor)

fun FieldLocation.toReference(): FieldReference =
  FieldReference(hostClass.toReference(), fieldName, fieldDescriptor)
