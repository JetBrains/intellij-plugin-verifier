package com.jetbrains.pluginverifier.location

import com.github.salomonbrys.kotson.jsonDeserializer
import com.github.salomonbrys.kotson.jsonSerializer
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonPrimitive
import com.jetbrains.pluginverifier.utils.CompactJson
import com.jetbrains.pluginverifier.utils.CompactJson.serialize
import com.jetbrains.pluginverifier.utils.MessageUtils

data class ClassPath(val type: Type, val path: String) {
  enum class Type { ROOT, CLASSES_DIRECTORY, JAR_FILE }
}

/**
 * @author Sergey Patrikeev
 */
interface ProblemLocation {

  companion object {
    fun fromClass(className: String, signature: String?, classPath: ClassPath): ClassLocation = ClassLocation(className, signature ?: "", classPath)

    fun fromMethod(hostClass: String, methodName: String, methodDescriptor: String, parameterNames: List<String>, signature: String?, classPath: ClassPath): MethodLocation
        = MethodLocation(hostClass, methodName, methodDescriptor, parameterNames, signature ?: "", classPath)

    fun fromField(hostClass: String, fieldName: String, fieldDescriptor: String, signature: String?, classPath: ClassPath): FieldLocation = FieldLocation(hostClass, fieldName, fieldDescriptor, signature ?: "", classPath)
  }

}

data class MethodLocation(val hostClass: String,
                          val methodName: String,
                          val methodDescriptor: String,
                          val parameterNames: List<String>,
                          val signature: String,
                          val classPath: ClassPath) : ProblemLocation {

  init {
    require(methodDescriptor.startsWith("(") && methodDescriptor.contains(")"), { methodDescriptor })
    require(parameterNames.size == MessageUtils.parseMethodParameters(methodDescriptor).size,
        { "Number of method descriptor parameters is not equal to number of parameter names: $methodDescriptor vs. $parameterNames" }
    )
  }

  override fun toString(): String = MessageUtils.convertMethod(methodName, methodDescriptor, hostClass, parameterNames)
}

data class FieldLocation(val hostClass: String,
                         val fieldName: String,
                         val fieldDescriptor: String,
                         val signature: String,
                         val classPath: ClassPath) : ProblemLocation {
  override fun toString(): String = MessageUtils.convertField(fieldName, hostClass)
}

data class ClassLocation(val className: String, val signature: String, val classPath: ClassPath) : ProblemLocation {
  override fun toString(): String = MessageUtils.convertClass(className)
}

internal val problemLocationSerializer = jsonSerializer<ProblemLocation> {
  val src = it.src
  fun serializeClassPath(classPath: ClassPath): String = "${classPath.type.name}|${classPath.path}"
  return@jsonSerializer when (src) {
    is MethodLocation -> JsonPrimitive(serialize(listOf("M", src.hostClass, src.methodName, src.methodDescriptor, src.parameterNames.joinToString("|"), src.signature, serializeClassPath(src.classPath))))
    is FieldLocation -> JsonPrimitive(serialize(listOf("F", src.hostClass, src.fieldName, src.fieldDescriptor, src.signature, serializeClassPath(src.classPath))))
    is ClassLocation -> JsonPrimitive(serialize(listOf("C", src.className, src.signature, serializeClassPath(src.classPath))))
    else -> throw IllegalArgumentException("Unregistered type ${it.src.javaClass.name}: ${it.src}")
  }
}

internal val problemLocationDeserializer = jsonDeserializer {
  val parts = CompactJson.deserialize(it.json.string)

  fun deserializeClassPath(classPath: String): ClassPath {
    val cpParts = classPath.split("|")
    return ClassPath(ClassPath.Type.valueOf(cpParts[0]), cpParts[1])
  }

  return@jsonDeserializer when {
    parts[0] == "M" -> ProblemLocation.fromMethod(parts[1], parts[2], parts[3], parts[4].split("|"), parts[5], deserializeClassPath(parts[6]))
    parts[0] == "F" -> ProblemLocation.fromField(parts[1], parts[2], parts[3], parts[4], deserializeClassPath(parts[5]))
    parts[0] == "C" -> ProblemLocation.fromClass(parts[1], parts[2], deserializeClassPath(parts[3]))
    else -> throw IllegalArgumentException("Unknown type ${it.json.string}")
  }
}