package com.jetbrains.pluginverifier.results.presentation

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.presentation.JvmDescriptorsPresentation.convertJvmDescriptorToNormalPresentation
import com.jetbrains.pluginverifier.results.presentation.JvmDescriptorsPresentation.convertTypeSignature
import com.jetbrains.pluginverifier.results.presentation.JvmDescriptorsPresentation.splitMethodDescriptorOnRawParametersAndReturnTypes

/**
 * Convert the simple binary class name of the possibly nested class to Java-like presentation,
 * preserving '$' dollar sign for anonymous classes.
 *
 * - SomeClass -> SomeClass
 * - SomeClass$Nested -> SomeClass.Nested
 * - SomeClass$Static$Inner -> SomeClass.Static.Inner
 * - SomeClass$Static$2 -> SomeClass.Static$2
 * - SomeClass$ -> SomeClass$
 * - SomeClass$4 -> SomeClass$4
 * - SomeClass$2$5 -> SomeClass$2$5
 * - SomeClass$321$XXX$555 -> SomeClass$321.XXX$555
 */
private fun String.convertSimpleClassName() =
    buildString {
      for (part in this@convertSimpleClassName.split("$")) {
        if (isNotEmpty()) {
          if (part.isEmpty() || part.first().isDigit()) {
            append("$")
          } else {
            append(".")
          }
        }
        append(part)
      }
    }

/**
 * Converts class name in binary form into Java-like presentation.
 * E.g. 'org/some/Class$Inner1$Inner2' -> 'org.some.Class.Inner1.Inner2'
 */
val toFullJavaClassName: (String) -> String = { binaryName -> binaryName.replace('/', '.').convertSimpleClassName() }

/**
 * Cuts off the package of the class and converts the simple name of the class to Java-like presentation
 * E.g. 'org/some/Class$Inner1$Inner2' -> 'Class.Inner1.Inner2'
 */
val toSimpleJavaClassName: (String) -> String = { binaryName -> binaryName.substringAfterLast("/").convertSimpleClassName() }

private fun FieldLocation.toFieldType(fieldTypeOption: FieldTypeOption): String {
  val descriptorConverter = when (fieldTypeOption) {
    FieldTypeOption.NO_TYPE -> return ""
    FieldTypeOption.SIMPLE_TYPE -> toSimpleJavaClassName
    FieldTypeOption.FULL_TYPE -> toFullJavaClassName
  }
  return if (signature.isNotEmpty()) {
    convertTypeSignature(signature, descriptorConverter)
  } else {
    convertJvmDescriptorToNormalPresentation(fieldDescriptor, descriptorConverter)
  }
}

fun ClassLocation.formatClassLocation(classLocationOption: ClassOption,
                                      classTypeSignatureOption: ClassGenericsSignatureOption): String {
  val converter = when (classLocationOption) {
    ClassOption.SIMPLE_NAME -> toSimpleJavaClassName
    ClassOption.FULL_NAME -> toFullJavaClassName
  }
  return if (signature.isNotEmpty()) {
    when (classTypeSignatureOption) {
      ClassGenericsSignatureOption.NO_GENERICS -> converter(className)
      ClassGenericsSignatureOption.WITH_GENERICS -> converter(className) + JvmDescriptorsPresentation.convertClassSignature(signature, toSimpleJavaClassName)
    }
  } else {
    converter(className)
  }
}

private fun ClassLocation.formatHostClass(hostClassOption: HostClassOption): String = when (hostClassOption) {
  HostClassOption.NO_HOST -> ""
  HostClassOption.SIMPLE_HOST_NAME -> formatClassLocation(ClassOption.SIMPLE_NAME, ClassGenericsSignatureOption.NO_GENERICS)
  HostClassOption.FULL_HOST_NAME -> formatClassLocation(ClassOption.FULL_NAME, ClassGenericsSignatureOption.NO_GENERICS)
  HostClassOption.FULL_HOST_WITH_SIGNATURE -> formatClassLocation(ClassOption.FULL_NAME, ClassGenericsSignatureOption.WITH_GENERICS)
}

fun MethodLocation.formatMethodLocation(hostClassOption: HostClassOption,
                                        methodParameterTypeOption: MethodParameterTypeOption,
                                        methodReturnTypeOption: MethodReturnTypeOption,
                                        methodParameterNameOption: MethodParameterNameOption): String = buildString {
  val formattedHost = hostClass.formatHostClass(hostClassOption)
  if (formattedHost.isNotEmpty()) {
    append("$formattedHost.")
  }
  append("$methodName(")
  val (paramAndNames, returnType) = methodParametersWithNamesAndReturnType(methodParameterTypeOption, methodReturnTypeOption, methodParameterNameOption)
  append(paramAndNames.joinToString())
  append(")")
  if (methodName != "<init>" && methodReturnTypeOption != MethodReturnTypeOption.NO_RETURN_TYPE) {
    append(" : $returnType")
  }
}

fun FieldLocation.formatFieldLocation(hostClassOption: HostClassOption, fieldTypeOption: FieldTypeOption): String = buildString {
  val formattedHost = hostClass.formatHostClass(hostClassOption)
  if (formattedHost.isNotEmpty()) {
    append("$formattedHost.")
  }
  append(fieldName)
  val type = toFieldType(fieldTypeOption)
  if (type.isNotEmpty()) {
    append(" : $type")
  }
}

private fun MethodLocation.zipWithNamesIfPossible(parametersTypes: List<String>): List<String> =
    if (parameterNames.size == parametersTypes.size) {
      parametersTypes.zip(parameterNames).map { "${it.first} ${it.second}" }
    } else {
      parametersTypes
    }

private fun MethodLocation.methodParametersWithNamesAndReturnType(methodParameterTypeOption: MethodParameterTypeOption,
                                                                  methodReturnTypeOption: MethodReturnTypeOption,
                                                                  methodParameterNameOption: MethodParameterNameOption): Pair<List<String>, String> {
  val paramsConverter = when (methodParameterTypeOption) {
    MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME -> toSimpleJavaClassName
    MethodParameterTypeOption.FULL_PARAM_CLASS_NAME -> toFullJavaClassName
  }
  val returnConverter = when (methodReturnTypeOption) {
    MethodReturnTypeOption.SIMPLE_RETURN_TYPE_CLASS_NAME -> toSimpleJavaClassName
    MethodReturnTypeOption.FULL_RETURN_TYPE_CLASS_NAME -> toFullJavaClassName
    MethodReturnTypeOption.NO_RETURN_TYPE -> toSimpleJavaClassName
  }
  val (parametersTypes, returnType) = if (signature.isNotEmpty()) {
    JvmDescriptorsPresentation.convertMethodSignature(signature, paramsConverter)
  } else {
    val (paramsTs, returnT) = splitMethodDescriptorOnRawParametersAndReturnTypes(methodDescriptor)
    (paramsTs.map { convertJvmDescriptorToNormalPresentation(it, paramsConverter) }) to (convertJvmDescriptorToNormalPresentation(returnT, returnConverter))
  }
  return when (methodParameterNameOption) {
    MethodParameterNameOption.NO_PARAMETER_NAMES -> parametersTypes to returnType
    MethodParameterNameOption.WITH_PARAM_NAMES_IF_AVAILABLE -> zipWithNamesIfPossible(parametersTypes) to returnType
  }
}