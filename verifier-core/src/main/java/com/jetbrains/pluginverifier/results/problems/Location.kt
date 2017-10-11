package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.results.presentation.Presentable

/**
 * @author Sergey Patrikeev
 */
interface Location : Presentable {

  companion object {
    fun fromClass(className: String,
                  signature: String?,
                  classPath: ClassPath,
                  accessFlags: AccessFlags): ClassLocation
        = ClassLocation(className, signature ?: "", classPath, accessFlags)

    fun fromMethod(hostClass: ClassLocation,
                   methodName: String,
                   methodDescriptor: String,
                   parameterNames: List<String>,
                   signature: String?,
                   accessFlags: AccessFlags): MethodLocation
        = MethodLocation(hostClass, methodName, methodDescriptor, parameterNames, signature ?: "", accessFlags)

    fun fromField(hostClass: ClassLocation,
                  fieldName: String,
                  fieldDescriptor: String,
                  signature: String?,
                  accessFlags: AccessFlags): FieldLocation
        = FieldLocation(hostClass, fieldName, fieldDescriptor, signature ?: "", accessFlags)
  }

}