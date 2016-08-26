package com.jetbrains.pluginverifier.reference

import com.github.salomonbrys.kotson.jsonDeserializer
import com.github.salomonbrys.kotson.jsonSerializer
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonPrimitive
import com.jetbrains.pluginverifier.utils.MessageUtils
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

/**
 * @author Sergey Patrikeev
 */
interface SymbolicReference {
  companion object {

    fun methodFrom(hostClass: String, methodName: String, methodDescriptor: String): MethodReference = MethodReference(hostClass, methodName, methodDescriptor)

    fun methodFrom(hostClass: ClassNode, methodName: String, methodDescriptor: String): MethodReference = methodFrom(hostClass.name, methodName, methodDescriptor)

    fun methodFrom(hostClass: String, method: MethodNode): MethodReference = methodFrom(hostClass, method.name, method.desc)

    fun methodFrom(hostClass: ClassNode, method: MethodNode): MethodReference = methodFrom(hostClass.name, method)

    fun fieldFrom(hostClass: ClassNode, field: FieldNode): FieldReference = fieldFrom(hostClass.name, field)

    fun fieldFrom(hostClass: String, field: FieldNode): FieldReference = fieldFrom(hostClass, field.name, field.desc)

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
  override fun toString(): String = MessageUtils.convertField(fieldName, fieldDescriptor, hostClass)

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