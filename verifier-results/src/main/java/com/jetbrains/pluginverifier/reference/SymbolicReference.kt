package com.jetbrains.pluginverifier.reference

import com.github.salomonbrys.kotson.jsonDeserializer
import com.github.salomonbrys.kotson.jsonSerializer
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonPrimitive
import com.jetbrains.pluginverifier.utils.MessageUtils

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
  override fun toString(): String = MessageUtils.convertMethod(methodName, methodDescriptor, hostClass)

}


data class FieldReference(val hostClass: String,
                          val fieldName: String,
                          val fieldDescriptor: String) : SymbolicReference {
  override fun toString(): String = MessageUtils.convertField(fieldName, hostClass)

}

data class ClassReference(val className: String) : SymbolicReference {
  override fun toString(): String = MessageUtils.convertClass(className)
}


internal val symbolicReferenceSerializer = jsonSerializer<SymbolicReference> {
  val src = it.src
  return@jsonSerializer when (src) {
    is MethodReference -> JsonPrimitive("M#${src.hostClass}#${src.methodName}#${src.methodDescriptor}")
    is FieldReference -> JsonPrimitive("F#${src.hostClass}#${src.fieldName}#${src.fieldDescriptor}")
    is ClassReference -> JsonPrimitive("C#${src.className}")
    else -> throw IllegalArgumentException("Unknwon type ${src.javaClass.name}: $src")
  }
}

internal val symbolicReferenceDeserializer = jsonDeserializer<SymbolicReference> {
  val parts = it.json.string.split('#')
  return@jsonDeserializer when {
    parts[0] == "M" -> SymbolicReference.methodFrom(parts[1], parts[2], parts[3])
    parts[0] == "F" -> SymbolicReference.fieldFrom(parts[1], parts[2], parts[3])
    parts[0] == "C" -> SymbolicReference.classFrom(parts[1])
    else -> throw IllegalArgumentException("Unknown type ${it.json.string}")
  }
}