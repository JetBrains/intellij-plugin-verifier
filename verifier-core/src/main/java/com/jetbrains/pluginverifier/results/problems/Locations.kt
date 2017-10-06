package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.utils.PresentationUtils
import com.jetbrains.pluginverifier.utils.PresentationUtils.convertClassSignature
import com.jetbrains.pluginverifier.utils.PresentationUtils.convertJvmDescriptorToNormalPresentation
import com.jetbrains.pluginverifier.utils.PresentationUtils.cutPackageConverter
import com.jetbrains.pluginverifier.utils.PresentationUtils.normalConverter
import com.jetbrains.pluginverifier.utils.PresentationUtils.splitMethodDescriptorOnRawParametersAndReturnTypes

data class MethodLocation(val hostClass: ClassLocation,
                          val methodName: String,
                          val methodDescriptor: String,
                          val parameterNames: List<String>,
                          val signature: String,
                          val accessFlags: AccessFlags) : Location {

  private fun zipWithNames(parametersTypes: List<String>): List<String> {
    val names: List<String> = if (parameterNames.size == parametersTypes.size) {
      parameterNames
    } else {
      (0..parametersTypes.size - 1).map { "arg$it" }
    }
    return parametersTypes.zip(names).map { "${it.first} ${it.second}" }
  }

  fun methodNameAndParameters(): String {
    val (parametersTypes, returnType) = if (signature.isNotEmpty()) {
      PresentationUtils.parseMethodSignature(signature, cutPackageConverter)
    } else {
      val (paramsTs, returnT) = splitMethodDescriptorOnRawParametersAndReturnTypes(methodDescriptor)
      (paramsTs.map { convertJvmDescriptorToNormalPresentation(it, cutPackageConverter) }) to (convertJvmDescriptorToNormalPresentation(returnT, cutPackageConverter))
    }
    val withNames = zipWithNames(parametersTypes)
    return "$methodName(${withNames.joinToString()}) : $returnType"
  }

  override fun toString(): String = "$hostClass.${methodNameAndParameters()}"
}

data class FieldLocation(val hostClass: ClassLocation,
                         val fieldName: String,
                         val fieldDescriptor: String,
                         val signature: String,
                         val accessFlags: AccessFlags) : Location {
  fun fieldNameAndType(): String {
    if (signature.isNotEmpty()) {
      return "$fieldName : ${PresentationUtils.convertFieldSignature(signature, cutPackageConverter)}"
    }
    val type = convertJvmDescriptorToNormalPresentation(fieldDescriptor, normalConverter)
    return "$fieldName : $type"
  }

  override fun toString(): String = "$hostClass.${fieldNameAndType()}"
}

data class ClassLocation(val className: String,
                         val signature: String,
                         val classPath: ClassPath,
                         val accessFlags: AccessFlags) : Location {
  override fun toString(): String {
    if (signature.isNotEmpty()) {
      return normalConverter(className) + convertClassSignature(signature, cutPackageConverter)
    }
    return normalConverter(className)
  }
}

