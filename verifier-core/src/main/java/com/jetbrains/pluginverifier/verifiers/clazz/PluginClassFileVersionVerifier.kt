package com.jetbrains.pluginverifier.verifiers.clazz

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.misc.toSystemIndependentName
import com.jetbrains.pluginverifier.results.problems.ClassFileVersionIncompatibleWithIde
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileOrigin
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

/**
 * MP-2147: Reject plugins with class file version > 8.
 */
class PluginClassFileVersionVerifier : ClassVerifier {

  companion object {
    const val MAXIMUM_IDE_SUPPORTED_CLASS_FILE_VERSION = Opcodes.V1_8
  }

  override fun verify(clazz: ClassNode, ctx: VerificationContext) {
    val classFileVersion = clazz.version
    if (classFileVersion > MAXIMUM_IDE_SUPPORTED_CLASS_FILE_VERSION) {
      val originOfClass = ctx.classResolver.getOriginOfClass(clazz.name)
      if (originOfClass is ClassFileOrigin.PluginClass) {
        val presentableClassPath = getPresentablePath(originOfClass.containingResolver)
        if (presentableClassPath != null) {
          ctx.registerProblem(
              ClassFileVersionIncompatibleWithIde(
                  presentableClassPath,
                  classFileVersion,
                  MAXIMUM_IDE_SUPPORTED_CLASS_FILE_VERSION
              )
          )
        }
      }
    }
  }

  private fun getPresentablePath(resolver: Resolver): String? {
    val singleFile = resolver.classPath.firstOrNull()
    if (singleFile != null) {
      val fullPath = singleFile.path.toSystemIndependentName()
      return if ("lib/" in fullPath) {
        "lib/" + fullPath.substringAfterLast("lib/")
      } else {
        fullPath.substringAfterLast("/")
      }
    }
    return null
  }
}