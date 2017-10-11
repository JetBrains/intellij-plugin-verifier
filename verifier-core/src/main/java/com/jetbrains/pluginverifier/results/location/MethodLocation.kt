package com.jetbrains.pluginverifier.results.location

import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.results.presentation.PresentationUtils

data class MethodLocation(val hostClass: ClassLocation,
                          val methodName: String,
                          val methodDescriptor: String,
                          val parameterNames: List<String>,
                          val signature: String,
                          val modifiers: Modifiers) : Location {
  override val shortPresentation: String = "$hostClass.${methodNameAndParameters(PresentationUtils.cutPackageConverter)}"

  override val fullPresentation: String = "$hostClass.${methodNameAndParameters(PresentationUtils.normalConverter)}"

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
      val (paramsTs, returnT) = PresentationUtils.splitMethodDescriptorOnRawParametersAndReturnTypes(methodDescriptor)
      (paramsTs.map { PresentationUtils.convertJvmDescriptorToNormalPresentation(it, descriptorConverter) }) to (PresentationUtils.convertJvmDescriptorToNormalPresentation(returnT, descriptorConverter))
    }
    val withNames = zipWithNames(parametersTypes)
    return "$methodName(${withNames.joinToString()}) : $returnType"
  }

  override fun toString(): String = shortPresentation
}