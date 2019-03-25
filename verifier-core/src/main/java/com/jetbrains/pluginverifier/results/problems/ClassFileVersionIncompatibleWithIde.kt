package com.jetbrains.pluginverifier.results.problems

import org.objectweb.asm.Opcodes
import java.util.*

class ClassFileVersionIncompatibleWithIde(
    val presentableClassPath: String,
    val classFileVersion: Int,
    val maximumIdeSupportedClassFileVersion: Int
) : CompatibilityProblem() {

  override val problemType
    get() = "Incompatible class file version"

  override val shortDescription: String
    get() = "Classes of $presentableClassPath have incompatible class file version $classFileVersion"

  override val fullDescription: String
    get() = "Classes of $presentableClassPath have class file version $classFileVersion (Java ${getJavaVersion(classFileVersion)}) " +
        "while IDE supports maximum version of $maximumIdeSupportedClassFileVersion (Java ${getJavaVersion(maximumIdeSupportedClassFileVersion)})"

  override fun equals(other: Any?) = other is ClassFileVersionIncompatibleWithIde
      && presentableClassPath == other.presentableClassPath
      && classFileVersion == other.classFileVersion
      && maximumIdeSupportedClassFileVersion == other.maximumIdeSupportedClassFileVersion

  override fun hashCode() =
      Objects.hash(presentableClassPath, classFileVersion, maximumIdeSupportedClassFileVersion)

  private fun getJavaVersion(classFileVersion: Int): String {
    if (classFileVersion == Opcodes.V1_1) {
      return "1.1"
    }
    val javaVersion = (classFileVersion and 0xFFFF) - 44
    if (classFileVersion > Opcodes.V1_8) {
      return javaVersion.toString()
    }
    return "1.$javaVersion"
  }

}