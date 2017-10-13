package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.core.VerificationResultHolder
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.location.classpath.ClassPath
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.results.problems.Problem
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

data class VerificationContext(
    val classLoader: Resolver,
    val resultHolder: VerificationResultHolder,
    val externalClassesPrefixes: List<String>
) {

  fun registerProblem(problem: Problem) {
    resultHolder.registerProblem(problem)
  }

  fun registerDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage) {
    resultHolder.registerDeprecatedUsage(deprecatedApiUsage)
  }

  fun isExternalClass(className: String): Boolean = externalClassesPrefixes.any { it.isNotEmpty() && className.startsWith(it) }

  private fun getClassPath(classNode: ClassNode): ClassPath {
    val className = classNode.name
    val actualResolver = classLoader.getClassLocation(className) ?: return ClassPath(ClassPath.Type.ROOT, "root")
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
      Location.fromClass(clazz.name, clazz.signature, getClassPath(clazz), Modifiers(clazz.access))

  fun fromMethod(hostClass: ClassNode, method: MethodNode): MethodLocation =
      Location.fromMethod(fromClass(hostClass), method.name, method.desc, method.getParameterNames(), method.signature, Modifiers(method.access))

  fun fromField(hostClass: ClassNode, field: FieldNode): FieldLocation =
      Location.fromField(fromClass(hostClass), field.name, field.desc, field.signature, Modifiers(field.access))

}