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

data class AccessFlags(val flags: Int) {

  enum class Flag(val flag: Int) {
    PUBLIC(0x0001), // class, field, method
    PRIVATE(0x0002), // class, field, method
    PROTECTED(0x0004), // class, field, method
    STATIC(0x0008), // field, method
    FINAL(0x0010), // class, field, method, parameter
    SUPER(0x0020), // class
    SYNCHRONIZED(0x0020), // method
    VOLATILE(0x0040), // field
    BRIDGE(0x0040), // method
    VARARGS(0x0080), // method
    TRANSIENT(0x0080), // field
    NATIVE(0x0100), // method
    INTERFACE(0x0200), // class
    ABSTRACT(0x0400), // class, method
    STRICT(0x0800), // method
    SYNTHETIC(0x1000), // class, field, method, parameter
    ANNOTATION(0x2000), // class
    ENUM(0x4000), // class(?) field inner
    MANDATED(0x8000), // parameter
    DEPRECATED(0x20000) // class, field, method
  }

  fun asSet(): Set<Flag> = Flag.values().filter { flags.and(it.flag) != 0 }.toSet()
}

/**
 * @author Sergey Patrikeev
 */
interface ProblemLocation {

  companion object {
    fun fromClass(className: String,
                  signature: String?,
                  classPath: ClassPath,
                  accessFlags: AccessFlags): ClassLocation = ClassLocation(className, signature ?: "", classPath, accessFlags)

    fun fromMethod(hostClass: String,
                   methodName: String,
                   methodDescriptor: String,
                   parameterNames: List<String>,
                   signature: String?,
                   classPath: ClassPath,
                   accessFlags: AccessFlags): MethodLocation
        = MethodLocation(hostClass, methodName, methodDescriptor, parameterNames, signature ?: "", classPath, accessFlags)

    fun fromField(hostClass: String,
                  fieldName: String,
                  fieldDescriptor: String,
                  signature: String?,
                  classPath: ClassPath,
                  accessFlags: AccessFlags): FieldLocation = FieldLocation(hostClass, fieldName, fieldDescriptor, signature ?: "", classPath, accessFlags)
  }

}

data class MethodLocation(val hostClass: String,
                          val methodName: String,
                          val methodDescriptor: String,
                          val parameterNames: List<String>,
                          val signature: String,
                          val classPath: ClassPath,
                          val accessFlags: AccessFlags) : ProblemLocation {

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
                         val classPath: ClassPath,
                         val accessFlags: AccessFlags) : ProblemLocation {
  override fun toString(): String = MessageUtils.convertField(fieldName, hostClass)
}

data class ClassLocation(val className: String, val signature: String, val classPath: ClassPath, val accessFlags: AccessFlags) : ProblemLocation {
  override fun toString(): String = MessageUtils.convertClass(className)
}

internal val problemLocationSerializer = jsonSerializer<ProblemLocation> {
  val src = it.src
  fun serializeClassPath(classPath: ClassPath): String = "${classPath.type.name}|${classPath.path}"
  fun serializeAccessFlags(accessFlags: AccessFlags): String = accessFlags.flags.toString()
  return@jsonSerializer when (src) {
    is MethodLocation -> JsonPrimitive(serialize(listOf("M", src.hostClass, src.methodName, src.methodDescriptor, src.parameterNames.joinToString("|"), src.signature, serializeClassPath(src.classPath), serializeAccessFlags(src.accessFlags))))
    is FieldLocation -> JsonPrimitive(serialize(listOf("F", src.hostClass, src.fieldName, src.fieldDescriptor, src.signature, serializeClassPath(src.classPath), serializeAccessFlags(src.accessFlags))))
    is ClassLocation -> JsonPrimitive(serialize(listOf("C", src.className, src.signature, serializeClassPath(src.classPath), serializeAccessFlags(src.accessFlags))))
    else -> throw IllegalArgumentException("Unregistered type ${it.src.javaClass.name}: ${it.src}")
  }
}

internal val problemLocationDeserializer = jsonDeserializer {
  val parts = CompactJson.deserialize(it.json.string)

  fun deserializeClassPath(classPath: String): ClassPath {
    val cpParts = classPath.split("|")
    return ClassPath(ClassPath.Type.valueOf(cpParts[0]), cpParts[1])
  }

  fun deserializeAccessFlags(flags: String) = AccessFlags(flags.toInt())

  return@jsonDeserializer when {
    parts[0] == "M" -> ProblemLocation.fromMethod(parts[1], parts[2], parts[3], parts[4].split("|"), parts[5], deserializeClassPath(parts[6]), deserializeAccessFlags(parts[7]))
    parts[0] == "F" -> ProblemLocation.fromField(parts[1], parts[2], parts[3], parts[4], deserializeClassPath(parts[5]), deserializeAccessFlags(parts[6]))
    parts[0] == "C" -> ProblemLocation.fromClass(parts[1], parts[2], deserializeClassPath(parts[3]), deserializeAccessFlags(parts[4]))
    else -> throw IllegalArgumentException("Unknown type ${it.json.string}")
  }
}