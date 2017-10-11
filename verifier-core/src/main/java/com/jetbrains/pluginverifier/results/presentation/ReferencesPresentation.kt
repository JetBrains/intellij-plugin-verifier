package com.jetbrains.pluginverifier.results.presentation

import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference

fun ClassReference.formatClassReference(classReferenceOption: ClassOption): String =
    when (classReferenceOption) {
      ClassOption.SIMPLE_NAME -> toSimpleJavaClassName(className)
      ClassOption.FULL_NAME -> toFullJavaClassName(className)
    }

fun MethodReference.formatMethodReference(hostClassOption: HostClassOption,
                                          methodParameterTypeOption: MethodParameterTypeOption,
                                          methodReturnTypeOption: MethodReturnTypeOption): String = buildString {
  val formattedHost = hostClass.formatHost(hostClassOption)
  if (formattedHost.isNotEmpty()) {
    append(formattedHost + ".")
  }
  append(methodName)
  val (params, returnType) = getMethodParametersAndReturnType(methodParameterTypeOption, methodReturnTypeOption)
  append("(" + params.joinToString() + ")")
  append(" : " + returnType)
}

fun FieldReference.formatFieldReference(hostClassOption: HostClassOption, fieldTypeOption: FieldTypeOption): String = buildString {
  val formattedHost = hostClass.formatHost(hostClassOption)
  if (formattedHost.isNotEmpty()) {
    append(formattedHost + ".")
  }
  append(fieldName)
  val fieldType = getFieldType(fieldTypeOption)
  if (fieldType.isNotEmpty()) {
    append(" : " + fieldType)
  }
}

private fun ClassReference.formatHost(hostClassOption: HostClassOption): String = when (hostClassOption) {
  HostClassOption.NO_HOST -> ""
  HostClassOption.SIMPLE_HOST_NAME -> toSimpleJavaClassName(className)
  HostClassOption.FULL_HOST_NAME -> toFullJavaClassName(className)
  HostClassOption.FULL_HOST_WITH_SIGNATURE -> toFullJavaClassName(className)
}

private fun FieldReference.getFieldType(fieldTypeOption: FieldTypeOption): String {
  val converter = when (fieldTypeOption) {
    FieldTypeOption.NO_HOST -> return ""
    FieldTypeOption.SIMPLE_HOST_NAME -> toSimpleJavaClassName
    FieldTypeOption.FULL_HOST_NAME -> toFullJavaClassName
  }
  return JvmDescriptorsPresentation.convertJvmDescriptorToNormalPresentation(fieldDescriptor, converter)
}

private fun MethodReference.getMethodParametersAndReturnType(methodParameterTypeOption: MethodParameterTypeOption,
                                                             methodReturnTypeOption: MethodReturnTypeOption): Pair<List<String>, String> {
  val paramConverter = if (methodParameterTypeOption == MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME) toSimpleJavaClassName else toFullJavaClassName
  val returnConverter = if (methodReturnTypeOption == MethodReturnTypeOption.SIMPLE_RETURN_TYPE_CLASS_NAME) toSimpleJavaClassName else toFullJavaClassName
  val (parametersTypes, rawReturnType) = JvmDescriptorsPresentation.splitMethodDescriptorOnRawParametersAndReturnTypes(methodDescriptor)
  val parameters = parametersTypes.map { JvmDescriptorsPresentation.convertJvmDescriptorToNormalPresentation(it, paramConverter) }
  val returnType = JvmDescriptorsPresentation.convertJvmDescriptorToNormalPresentation(rawReturnType, returnConverter)
  return parameters to returnType
}