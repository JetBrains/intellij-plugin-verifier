package com.jetbrains.pluginverifier.reference

import com.github.salomonbrys.kotson.jsonDeserializer
import com.github.salomonbrys.kotson.jsonSerializer
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonPrimitive
import com.jetbrains.pluginverifier.utils.CompactJsonUtil
import com.jetbrains.pluginverifier.utils.PresentationUtils.convertJvmDescriptorToNormalPresentation
import com.jetbrains.pluginverifier.utils.PresentationUtils.cutPackageConverter
import com.jetbrains.pluginverifier.utils.PresentationUtils.normalConverter
import com.jetbrains.pluginverifier.utils.PresentationUtils.splitMethodDescriptorOnRawParametersAndReturnTypes

/**
 * @author Sergey Patrikeev
 */
interface SymbolicReference {
  companion object {

    fun methodFrom(hostClass: String, methodName: String, methodDescriptor: String): MethodReference = MethodReference(hostClass, methodName, methodDescriptor)

    fun fieldFrom(hostClass: String, fieldName: String, fieldDescriptor: String): FieldReference = FieldReference(hostClass, fieldName, fieldDescriptor)

    fun classFrom(className: String): ClassReference = ClassReference(className)
  }
}

data class MethodReference(val hostClass: String,
                           val methodName: String,
                           val methodDescriptor: String) : SymbolicReference {
  override fun toString(): String {
    val (parametersTypes, returnType) = splitMethodDescriptorOnRawParametersAndReturnTypes(methodDescriptor)
    val (presentableParams, presentableReturn) = (parametersTypes.map { convertJvmDescriptorToNormalPresentation(it, cutPackageConverter) }) to (convertJvmDescriptorToNormalPresentation(returnType, cutPackageConverter))
    return normalConverter(hostClass) + ".$methodName" + "(" + presentableParams.joinToString() + ") : $presentableReturn"
  }

}


data class FieldReference(val hostClass: String,
                          val fieldName: String,
                          val fieldDescriptor: String) : SymbolicReference {
  override fun toString(): String {
    val type = convertJvmDescriptorToNormalPresentation(fieldDescriptor, cutPackageConverter)
    return normalConverter(hostClass) + ".$fieldName : $type"
  }

}

data class ClassReference(val className: String) : SymbolicReference {
  override fun toString(): String = normalConverter(className)
}


internal val symbolicReferenceSerializer = jsonSerializer<SymbolicReference> {
  val src = it.src
  return@jsonSerializer when (src) {
    is MethodReference -> JsonPrimitive(CompactJsonUtil.serialize(listOf("M", src.hostClass, src.methodName, src.methodDescriptor)))
    is FieldReference -> JsonPrimitive(CompactJsonUtil.serialize(listOf("F", src.hostClass, src.fieldName, src.fieldDescriptor)))
    is ClassReference -> JsonPrimitive(CompactJsonUtil.serialize(listOf("C", src.className)))
    else -> throw IllegalArgumentException("Unknown type ${src.javaClass.name}: $src")
  }
}

internal val symbolicReferenceDeserializer = jsonDeserializer {
  val parts = CompactJsonUtil.deserialize(it.json.string)
  return@jsonDeserializer when {
    parts[0] == "M" -> SymbolicReference.methodFrom(parts[1], parts[2], parts[3])
    parts[0] == "F" -> SymbolicReference.fieldFrom(parts[1], parts[2], parts[3])
    parts[0] == "C" -> SymbolicReference.classFrom(parts[1])
    else -> throw IllegalArgumentException("Unknown type ${it.json.string}")
  }
}