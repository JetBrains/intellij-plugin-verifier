package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.results.presentation.PresentationUtils
import com.jetbrains.pluginverifier.results.presentation.PresentationUtils.convertClassSignature
import com.jetbrains.pluginverifier.results.presentation.PresentationUtils.convertJvmDescriptorToNormalPresentation
import com.jetbrains.pluginverifier.results.presentation.PresentationUtils.cutPackageConverter
import com.jetbrains.pluginverifier.results.presentation.PresentationUtils.normalConverter
import com.jetbrains.pluginverifier.results.presentation.PresentationUtils.splitMethodDescriptorOnRawParametersAndReturnTypes

data class MethodLocation(val hostClass: ClassLocation,
                          val methodName: String,
                          val methodDescriptor: String,
                          val parameterNames: List<String>,
                          val signature: String,
                          val accessFlags: AccessFlags) : Location {
  override val shortPresentation: String = "$hostClass.${methodNameAndParameters(cutPackageConverter)}"

  override val fullPresentation: String = "$hostClass.${methodNameAndParameters(normalConverter)}"

  private fun zipWithNames(parametersTypes: List<String>): List<String> {
    val names: List<String> = if (parameterNames.size == parametersTypes.size) {
      parameterNames
    } else {
      (0..parametersTypes.size - 1).map { "arg$it" }
    }
    return parametersTypes.zip(names).map { "${it.first} ${it.second}" }
  }

  fun methodNameAndParameters(descriptorConverter: (String) -> String): String {
    val (parametersTypes, returnType) = if (signature.isNotEmpty()) {
      PresentationUtils.parseMethodSignature(signature, descriptorConverter)
    } else {
      val (paramsTs, returnT) = splitMethodDescriptorOnRawParametersAndReturnTypes(methodDescriptor)
      (paramsTs.map { convertJvmDescriptorToNormalPresentation(it, descriptorConverter) }) to (convertJvmDescriptorToNormalPresentation(returnT, descriptorConverter))
    }
    val withNames = zipWithNames(parametersTypes)
    return "$methodName(${withNames.joinToString()}) : $returnType"
  }

  override fun toString(): String = shortPresentation
}

data class FieldLocation(val hostClass: ClassLocation,
                         val fieldName: String,
                         val fieldDescriptor: String,
                         val signature: String,
                         val accessFlags: AccessFlags) : Location {
  override val shortPresentation: String = "$hostClass.${fieldNameAndType(cutPackageConverter)}"

  override val fullPresentation: String = "$hostClass.${fieldNameAndType(normalConverter)}"

  private fun fieldNameAndType(descriptorConverter: (String) -> String): String {
    if (signature.isNotEmpty()) {
      return "$fieldName : ${PresentationUtils.convertFieldSignature(signature, descriptorConverter)}"
    }
    val type = convertJvmDescriptorToNormalPresentation(fieldDescriptor, descriptorConverter)
    return "$fieldName : $type"
  }

  override fun toString(): String = shortPresentation
}

data class ClassLocation(val className: String,
                         val signature: String,
                         val classPath: ClassPath,
                         val accessFlags: AccessFlags) : Location {
  override val shortPresentation: String = cutPackageConverter(className)

  override val fullPresentation: String = if (signature.isNotEmpty()) {
    normalConverter(className) + convertClassSignature(signature, cutPackageConverter)
  } else {
    normalConverter(className)
  }

  override fun toString(): String = fullPresentation
}

