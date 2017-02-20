package com.jetbrains.pluginverifier.location

import com.github.salomonbrys.kotson.jsonDeserializer
import com.github.salomonbrys.kotson.jsonSerializer
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonPrimitive
import com.jetbrains.pluginverifier.utils.CompactJson
import com.jetbrains.pluginverifier.utils.CompactJson.serialize
import com.jetbrains.pluginverifier.utils.PresentationUtils
import com.jetbrains.pluginverifier.utils.PresentationUtils.convertClassSignature
import com.jetbrains.pluginverifier.utils.PresentationUtils.convertJvmDescriptorToNormalPresentation
import com.jetbrains.pluginverifier.utils.PresentationUtils.cutPackageConverter
import com.jetbrains.pluginverifier.utils.PresentationUtils.normalConverter
import com.jetbrains.pluginverifier.utils.PresentationUtils.splitMethodDescriptorOnRawParametersAndReturnTypes

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

  fun contains(flag: Flag): Boolean = flags.and(flag.flag) != 0

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
                  accessFlags: AccessFlags): ClassLocation
        = ClassLocation(className, signature ?: "", classPath, accessFlags)

    fun fromMethod(hostClass: ClassLocation,
                   methodName: String,
                   methodDescriptor: String,
                   parameterNames: List<String>,
                   signature: String?,
                   accessFlags: AccessFlags): MethodLocation
        = MethodLocation(hostClass, methodName, methodDescriptor, parameterNames, signature ?: "", accessFlags)

    fun fromField(hostClass: ClassLocation,
                  fieldName: String,
                  fieldDescriptor: String,
                  signature: String?,
                  accessFlags: AccessFlags): FieldLocation
        = FieldLocation(hostClass, fieldName, fieldDescriptor, signature ?: "", accessFlags)
  }

}

data class MethodLocation(val hostClass: ClassLocation,
                          val methodName: String,
                          val methodDescriptor: String,
                          val parameterNames: List<String>,
                          val signature: String,
                          val accessFlags: AccessFlags) : ProblemLocation {

  private fun zipWithNames(parametersTypes: List<String>): List<String> {
    val names: List<String> = if (parameterNames.size == parametersTypes.size) {
      parameterNames
    } else {
      (0..parametersTypes.size - 1).map { "arg$it" }
    }
    return parametersTypes.zip(names).map { "${it.first} ${it.second}" }
  }

  override fun toString(): String {
    val (parametersTypes, returnType) = if (signature.isNotEmpty()) {
      PresentationUtils.parseMethodSignature(signature, cutPackageConverter)
    } else {
      val (paramsTs, returnT) = splitMethodDescriptorOnRawParametersAndReturnTypes(methodDescriptor)
      (paramsTs.map { convertJvmDescriptorToNormalPresentation(it, cutPackageConverter) }) to (convertJvmDescriptorToNormalPresentation(returnT, cutPackageConverter))
    }
    val withNames = zipWithNames(parametersTypes)
    return hostClass.toString() + ".$methodName" + "(" + withNames.joinToString() + ") : $returnType"
  }
}

data class FieldLocation(val hostClass: ClassLocation,
                         val fieldName: String,
                         val fieldDescriptor: String,
                         val signature: String,
                         val accessFlags: AccessFlags) : ProblemLocation {
  override fun toString(): String {
    if (signature.isNotEmpty()) {
      return hostClass.toString() + ".$fieldName : ${PresentationUtils.convertFieldSignature(signature, cutPackageConverter)}"
    }
    val type = convertJvmDescriptorToNormalPresentation(fieldDescriptor, normalConverter)
    return hostClass.toString() + ".$fieldName : $type"
  }
}

data class ClassLocation(val className: String,
                         val signature: String,
                         val classPath: ClassPath,
                         val accessFlags: AccessFlags) : ProblemLocation {
  override fun toString(): String {
    if (signature.isNotEmpty()) {
      return normalConverter(className) + convertClassSignature(signature, cutPackageConverter)
    }
    return normalConverter(className)
  }
}

internal val problemLocationSerializer = jsonSerializer<ProblemLocation> {
  val src = it.src

  fun serializeClassPath(classPath: ClassPath): String = "${classPath.type.name}|${classPath.path}"

  fun serializeAccessFlags(accessFlags: AccessFlags): String = accessFlags.flags.toString()

  fun serializeClassLocation(src: ClassLocation): String =
      serialize(listOf("C", src.className, src.signature, serializeClassPath(src.classPath), serializeAccessFlags(src.accessFlags)))

  return@jsonSerializer when (src) {
    is MethodLocation -> JsonPrimitive(serialize(listOf("M", serializeClassLocation(src.hostClass), src.methodName, src.methodDescriptor, src.parameterNames.joinToString("|"), src.signature, serializeAccessFlags(src.accessFlags))))
    is FieldLocation -> JsonPrimitive(serialize(listOf("F", serializeClassLocation(src.hostClass), src.fieldName, src.fieldDescriptor, src.signature, serializeAccessFlags(src.accessFlags))))
    is ClassLocation -> JsonPrimitive(serializeClassLocation(src))
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

  fun deserializeClassLocation(string: String): ClassLocation {
    val classParts = CompactJson.deserialize(string)
    return ProblemLocation.fromClass(classParts[1], classParts[2], deserializeClassPath(classParts[3]), deserializeAccessFlags(classParts[4]))
  }

  return@jsonDeserializer when {
    parts[0] == "M" -> ProblemLocation.fromMethod(deserializeClassLocation(parts[1]), parts[2], parts[3], parts[4].split("|"), parts[5], deserializeAccessFlags(parts[6]))
    parts[0] == "F" -> ProblemLocation.fromField(deserializeClassLocation(parts[1]), parts[2], parts[3], parts[4], deserializeAccessFlags(parts[5]))
    parts[0] == "C" -> deserializeClassLocation(it.json.string)
    else -> throw IllegalArgumentException("Unknown type ${it.json.string}")
  }
}