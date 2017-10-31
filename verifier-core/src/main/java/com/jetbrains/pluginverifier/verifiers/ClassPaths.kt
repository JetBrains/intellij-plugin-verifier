package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.location.classpath.ClassPath
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

fun Resolver.getClassPath(className: String): ClassPath {
  val actualResolver = getClassLocation(className) ?: return ClassPath(ClassPath.Type.ROOT, "root")
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

fun VerificationContext.fromClass(clazz: ClassNode): ClassLocation =
    classLocationByClassNode(clazz, classLoader)

fun classLocationByClassNode(clazz: ClassNode, classLoader: Resolver) =
    Location.fromClass(clazz.name, clazz.signature, classLoader.getClassPath(clazz.name), Modifiers(clazz.access))

fun VerificationContext.fromMethod(hostClass: ClassNode, method: MethodNode): MethodLocation =
    methodLocationByClassAndMethodNodes(hostClass, method, classLoader)

fun methodLocationByClassAndMethodNodes(hostClass: ClassNode, method: MethodNode, classLoader: Resolver) =
    Location.fromMethod(classLocationByClassNode(hostClass, classLoader), method.name, method.desc, method.getParameterNames(), method.signature, Modifiers(method.access))

fun VerificationContext.fromField(hostClass: ClassNode, field: FieldNode): FieldLocation =
    fieldLocationByFieldAndClassNodes(hostClass, field, classLoader)

fun fieldLocationByFieldAndClassNodes(hostClass: ClassNode, field: FieldNode, classLoader: Resolver) =
    Location.fromField(classLocationByClassNode(hostClass, classLoader), field.name, field.desc, field.signature, Modifiers(field.access))
