package com.jetbrains.pluginverifier.results.reference

import com.jetbrains.pluginverifier.results.presentation.PresentationUtils.convertJvmDescriptorToNormalPresentation
import com.jetbrains.pluginverifier.results.presentation.PresentationUtils.cutPackageConverter
import com.jetbrains.pluginverifier.results.presentation.PresentationUtils.normalConverter
import com.jetbrains.pluginverifier.results.presentation.PresentationUtils.splitMethodDescriptorOnRawParametersAndReturnTypes

/**
 * @author Sergey Patrikeev
 */
interface SymbolicReference {
  companion object {

    fun methodOf(hostClass: String, methodName: String, methodDescriptor: String): MethodReference = MethodReference(ClassReference(hostClass), methodName, methodDescriptor)

    fun fieldOf(hostClass: String, fieldName: String, fieldDescriptor: String): FieldReference = FieldReference(ClassReference(hostClass), fieldName, fieldDescriptor)

    fun classOf(className: String): ClassReference = ClassReference(className)
  }
}

data class MethodReference(val hostClass: ClassReference,
                           val methodName: String,
                           val methodDescriptor: String) : SymbolicReference {
  override fun toString(): String = "$hostClass.${methodNameAndParameters()}"

  fun methodNameAndParameters(): String {
    val (parametersTypes, returnType) = splitMethodDescriptorOnRawParametersAndReturnTypes(methodDescriptor)
    val (presentableParams, presentableReturn) = (parametersTypes.map { convertJvmDescriptorToNormalPresentation(it, cutPackageConverter) }) to (convertJvmDescriptorToNormalPresentation(returnType, cutPackageConverter))
    return "$methodName(" + presentableParams.joinToString() + ") : $presentableReturn"
  }

}


data class FieldReference(val hostClass: ClassReference,
                          val fieldName: String,
                          val fieldDescriptor: String) : SymbolicReference {

  fun fieldNameAndParameters(): String = "$fieldName : ${convertJvmDescriptorToNormalPresentation(fieldDescriptor, cutPackageConverter)}"

  override fun toString(): String = "$hostClass.${fieldNameAndParameters()}"

}

data class ClassReference(val className: String) : SymbolicReference {
  override fun toString(): String = normalConverter(className)
}