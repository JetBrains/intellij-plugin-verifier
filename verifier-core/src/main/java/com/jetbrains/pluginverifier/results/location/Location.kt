package com.jetbrains.pluginverifier.results.location

import com.jetbrains.pluginverifier.results.location.classpath.ClassPath
import com.jetbrains.pluginverifier.results.modifiers.Modifiers

interface Location {

  companion object {
    fun fromClass(className: String,
                  signature: String?,
                  classPath: ClassPath,
                  modifiers: Modifiers): ClassLocation
        = ClassLocation(className, signature ?: "", classPath, modifiers)

    fun fromMethod(hostClass: ClassLocation,
                   methodName: String,
                   methodDescriptor: String,
                   parameterNames: List<String>,
                   signature: String?,
                   modifiers: Modifiers): MethodLocation
        = MethodLocation(hostClass, methodName, methodDescriptor, parameterNames, signature ?: "", modifiers)

    fun fromField(hostClass: ClassLocation,
                  fieldName: String,
                  fieldDescriptor: String,
                  signature: String?,
                  modifiers: Modifiers): FieldLocation
        = FieldLocation(hostClass, fieldName, fieldDescriptor, signature ?: "", modifiers)
  }

}