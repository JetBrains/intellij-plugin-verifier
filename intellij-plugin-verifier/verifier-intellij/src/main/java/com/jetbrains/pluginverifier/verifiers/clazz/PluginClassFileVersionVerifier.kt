package com.jetbrains.pluginverifier.verifiers.clazz

import com.jetbrains.plugin.structure.base.utils.toSystemIndependentName
import com.jetbrains.pluginverifier.results.problems.ClassFileVersionIncompatibleWithIde
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.IntelliJClassFileOrigin
import java.nio.file.Path

/**
 * MP-2147: Reject plugins with class file version > 8.
 */
class PluginClassFileVersionVerifier : ClassVerifier {

  companion object {
    const val MAXIMUM_IDE_SUPPORTED_VERSION = 8
  }

  override fun verify(classFile: ClassFile, context: VerificationContext) {
    val javaVersion = classFile.javaVersion
    if (javaVersion > MAXIMUM_IDE_SUPPORTED_VERSION) {
      val classOrigin = classFile.classFileOrigin
      if (classOrigin is IntelliJClassFileOrigin.PluginClass) {
        val presentableClassPath = getPresentablePath(classOrigin.classPath)
        if (presentableClassPath != null) {
          context.problemRegistrar.registerProblem(
              ClassFileVersionIncompatibleWithIde(
                  presentableClassPath,
                  javaVersion,
                  MAXIMUM_IDE_SUPPORTED_VERSION
              )
          )
        }
      }
    }
  }

  private fun getPresentablePath(classPath: Path): String? {
    val fullPath = classPath.toString().toSystemIndependentName()
    return if ("lib/" in fullPath) {
      "lib/" + fullPath.substringAfterLast("lib/")
    } else {
      fullPath.substringAfterLast("/")
    }
  }
}