package com.jetbrains.pluginverifier.utils

import com.intellij.structure.ide.Ide
import com.intellij.structure.plugin.Plugin
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VerifierParams
import com.jetbrains.pluginverifier.location.*
import com.jetbrains.pluginverifier.problems.Problem
import com.jetbrains.pluginverifier.warnings.Warning
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.FieldNode
import org.jetbrains.intellij.plugins.internal.asm.tree.MethodNode

data class VerificationContext(
    val plugin: Plugin,
    val ide: Ide,
    val verifierParams: VerifierParams,
    val resolver: Resolver
) {
  val problems: MutableSet<Problem> = hashSetOf()

  val warnings: MutableSet<Warning> = hashSetOf()

  fun registerProblem(problem: Problem) {
    if (verifierParams.problemFilter.isRelevantProblem(plugin, problem)) {
      problems.add(problem)
    }
  }

  fun registerWarning(warning: Warning) {
    warnings.add(warning)
  }

  private fun getClassPath(classNode: ClassNode): ClassPath {
    val className = classNode.name
    val actualResolver = resolver.getClassLocation(className) ?: return ClassPath(ClassPath.Type.ROOT, "root")
    if (actualResolver.classPath.size != 1) {
      //it should not happen, because actually each class-file is coming from a specific resolver
      //(a specific jar file or maybe a `classes` directory)
      //but nevertheless process it as being a root of the plugin
      return ClassPath(ClassPath.Type.ROOT, "root")
    }
    val file = actualResolver.classPath.single()
    if (file.name.endsWith(".jar")) {
      val parentFile = file.parentFile
      if (parentFile != null && parentFile.isDirectory && parentFile.name == "lib") {
        //we only want to remember jar files from the <plugin>/lib/ directory and
        //don't want a name of the plugin file, because it's unspecified and could be something
        //like update1234.jar instead of original file name (as defined by the author)
        return ClassPath(ClassPath.Type.JAR_FILE, file.name)
      }
      return ClassPath(ClassPath.Type.ROOT, file.name)
    }
    if (file.isDirectory && file.name == "classes") {
      return ClassPath(ClassPath.Type.CLASSES_DIRECTORY, "classes")
    }
    return ClassPath(ClassPath.Type.ROOT, file.name)
  }

  fun fromClass(clazz: ClassNode): ClassLocation =
      Location.fromClass(clazz.name, clazz.signature, getClassPath(clazz), AccessFlags(clazz.access))

  fun fromMethod(hostClass: ClassNode, method: MethodNode): MethodLocation =
      Location.fromMethod(fromClass(hostClass), method.name, method.desc, VerifierUtil.getParameterNames(method), method.signature, AccessFlags(method.access))

  fun fromField(hostClass: ClassNode, field: FieldNode): FieldLocation =
      Location.fromField(fromClass(hostClass), field.name, field.desc, field.signature, AccessFlags(field.access))

}