package com.jetbrains.pluginverifier.results.reference

import com.jetbrains.pluginverifier.results.presentation.Presentable
import com.jetbrains.pluginverifier.results.presentation.PresentationUtils.convertJvmDescriptorToNormalPresentation
import com.jetbrains.pluginverifier.results.presentation.PresentationUtils.cutPackageConverter
import com.jetbrains.pluginverifier.results.presentation.PresentationUtils.normalConverter
import com.jetbrains.pluginverifier.results.presentation.PresentationUtils.splitMethodDescriptorOnRawParametersAndReturnTypes

/**
 * @author Sergey Patrikeev
 */
interface SymbolicReference : Presentable {
  companion object {

    fun methodOf(hostClass: String, methodName: String, methodDescriptor: String): MethodReference = MethodReference(ClassReference(hostClass), methodName, methodDescriptor)

    fun fieldOf(hostClass: String, fieldName: String, fieldDescriptor: String): FieldReference = FieldReference(ClassReference(hostClass), fieldName, fieldDescriptor)

    fun classOf(className: String): ClassReference = ClassReference(className)
  }
}

data class MethodReference(val hostClass: ClassReference,
                           val methodName: String,
                           val methodDescriptor: String) : SymbolicReference {

  override val shortPresentation: String = "$hostClass.${methodNameAndParameters(cutPackageConverter)}"

  override val fullPresentation: String = "$hostClass.${methodNameAndParameters(normalConverter)}"

  override fun toString(): String = shortPresentation

  private fun methodNameAndParameters(descriptorConverter: (String) -> String): String {
    val (parametersTypes, returnType) = splitMethodDescriptorOnRawParametersAndReturnTypes(methodDescriptor)
    val (presentableParams, presentableReturn) = (parametersTypes.map { convertJvmDescriptorToNormalPresentation(it, descriptorConverter) }) to (convertJvmDescriptorToNormalPresentation(returnType, descriptorConverter))
    return "$methodName(" + presentableParams.joinToString() + ") : $presentableReturn"
  }

}


data class FieldReference(val hostClass: ClassReference,
                          val fieldName: String,
                          val fieldDescriptor: String) : SymbolicReference {

  override val shortPresentation: String = fieldNameAndParameters(cutPackageConverter)

  override val fullPresentation: String = fieldNameAndParameters(normalConverter)

  private fun fieldNameAndParameters(descriptorConverter: (String) -> String): String =
      "$fieldName : ${convertJvmDescriptorToNormalPresentation(fieldDescriptor, descriptorConverter)}"

  override fun toString(): String = shortPresentation

}

data class ClassReference(val className: String) : SymbolicReference {
  override val shortPresentation: String = cutPackageConverter(className)

  override val fullPresentation: String = normalConverter(className)

  override fun toString(): String = fullPresentation
}