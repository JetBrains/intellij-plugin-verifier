package com.jetbrains.pluginverifier.utils

import com.jetbrains.pluginverifier.location.Location
import com.jetbrains.pluginverifier.problems.AccessType
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.problems.IllegalClassAccessProblem
import com.jetbrains.pluginverifier.reference.ClassReference
import com.jetbrains.pluginverifier.warnings.Warning
import org.jetbrains.intellij.plugins.internal.asm.Opcodes
import org.jetbrains.intellij.plugins.internal.asm.Type
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.FieldNode
import org.jetbrains.intellij.plugins.internal.asm.tree.LocalVariableNode
import org.jetbrains.intellij.plugins.internal.asm.tree.MethodNode

sealed class ClsResolution {
  object NotFound : ClsResolution()
  object ExternalClass : ClsResolution()
  class IllegalAccess(val resolvedNode: ClassNode, val accessType: AccessType) : ClsResolution()
  class Found(val node: ClassNode) : ClsResolution()
}

/**
 * To resolve an unresolved symbolic reference from D to a class or interface C denoted by N, the following steps are performed:
 * ...<JVM-related stuff>...
 *  3) Finally, access permissions to C are checked.
 *  If C is not accessible (§5.4.4) to D, class or interface resolution throws an IllegalAccessError.
 */
fun VerificationContext.resolveClass(className: String, lookup: ClassNode): ClsResolution {
  if (verifierParams.isExternalClass(className)) {
    return ClsResolution.ExternalClass
  }
  val node = findClassNode(className)
  if (node != null) {
    return if (VerifierUtil.isClassAccessibleToOtherClass(node, lookup)) {
      ClsResolution.Found(node)
    } else {
      ClsResolution.IllegalAccess(node, VerifierUtil.getAccessType(node.access))
    }
  }
  return ClsResolution.NotFound
}


fun VerificationContext.resolveClassOrProblem(className: String,
                                              lookup: ClassNode,
                                              lookupLocation: () -> Location): ClassNode? {
  val resolution = this.resolveClass(className, lookup)
  return when (resolution) {
    ClsResolution.NotFound -> {
      registerProblem(ClassNotFoundProblem(ClassReference(className), lookupLocation.invoke()))
      null
    }
    ClsResolution.ExternalClass -> null
    is ClsResolution.IllegalAccess -> {
      registerProblem(IllegalClassAccessProblem(fromClass(resolution.resolvedNode), resolution.accessType, lookupLocation.invoke()))
      null
    }
    is ClsResolution.Found -> resolution.node
  }
}

private fun VerificationContext.findClassNode(className: String): ClassNode? {
  try {
    return resolver.findClass(className)
  } catch (e: Exception) {
    registerWarning(Warning("Unable to read a class $className using ASM (<a href=\"http://asm.ow2.org\"></a>). Probably it has invalid class-file. Try to recompile the plugin"))
    return null
  }

}

fun VerificationContext.checkClassExistsOrExternal(className: String, registerMissing: () -> Location) {
  if (!verifierParams.isExternalClass(className) && !resolver.containsClass(className)) {
    registerProblem(ClassNotFoundProblem(ClassReference(className), registerMissing.invoke()))
  }
}


object VerifierUtil {

  @Suppress("UNCHECKED_CAST")
  fun getParameterNames(method: MethodNode): List<String> {
    val arguments = Type.getArgumentTypes(method.desc)
    val argumentsNumber = arguments.size
    val offset = if (VerifierUtil.isStatic(method)) 0 else 1
    var parameterNames: List<String> = emptyList()
    if (method.localVariables != null) {
      parameterNames = (method.localVariables as List<LocalVariableNode>).map { it.name }.drop(offset).take(argumentsNumber)
    }
    if (parameterNames.size != argumentsNumber) {
      parameterNames = (0..argumentsNumber - 1).map { "arg$it" }
    }
    return parameterNames
  }

  /**
   * @param descr full descriptor (may be an array type or a primitive type)
   *
   * @return null for primitive types and the innermost type for array types
   */
  fun extractClassNameFromDescr(descr: String): String? {
    //prepare array name
    val descr1 = descr.trimStart('[')

    if (isPrimitiveType(descr1)) return null

    if (descr1.startsWith("L") && descr1.endsWith(";")) {
      return descr1.substring(1, descr1.length - 1)
    }

    return descr1
  }

  private fun isPrimitiveType(type: String): Boolean = type.length == 1 && type.first() in "ZIJBFSDC"

  /**
   * A method is signature polymorphic if all of the following are true:
   * - It is declared in the java.lang.invoke.MethodHandle class.
   * - It has a single formal parameter of type Object[].
   * - It has a return type of Object.
   * - It has the ACC_VARARGS and ACC_NATIVE flags set.
   *
   * In Java SE 8, the only signature polymorphic methods are the invoke and invokeExact methods of the class java.lang.invoke.MethodHandle.
   */
  fun isSignaturePolymorphic(hostClass: String, methodNode: MethodNode): Boolean =
      "java/lang/invoke/MethodHandle" == hostClass
          && "([Ljava/lang/Object;)Ljava/lang/Object;" == methodNode.desc
          && isVararg(methodNode)
          && isNative(methodNode)

  fun getAccessType(accessFlags: Int): AccessType = when {
    accessFlags and Opcodes.ACC_PUBLIC != 0 -> AccessType.PUBLIC
    accessFlags and Opcodes.ACC_PRIVATE != 0 -> AccessType.PRIVATE
    accessFlags and Opcodes.ACC_PROTECTED != 0 -> AccessType.PROTECTED
    else -> AccessType.PACKAGE_PRIVATE
  }

  fun isVararg(methodNode: MethodNode): Boolean = methodNode.access and Opcodes.ACC_VARARGS != 0

  fun isNative(methodNode: MethodNode): Boolean = methodNode.access and Opcodes.ACC_NATIVE != 0

  fun isFinal(classNode: ClassNode): Boolean = classNode.access and Opcodes.ACC_FINAL != 0

  fun isFinal(superMethod: MethodNode): Boolean = superMethod.access and Opcodes.ACC_FINAL != 0

  fun isFinal(fieldNode: FieldNode): Boolean = fieldNode.access and Opcodes.ACC_FINAL != 0

  fun isInterface(classNode: ClassNode): Boolean = classNode.access and Opcodes.ACC_INTERFACE != 0

  fun isAbstract(clazz: ClassNode): Boolean = clazz.access and Opcodes.ACC_ABSTRACT != 0

  fun isPrivate(method: MethodNode): Boolean = method.access and Opcodes.ACC_PRIVATE != 0

  fun isPrivate(field: FieldNode): Boolean = field.access and Opcodes.ACC_PRIVATE != 0

  fun isPublic(classNode: ClassNode): Boolean = classNode.access and Opcodes.ACC_PUBLIC != 0

  fun isPublic(method: MethodNode): Boolean = method.access and Opcodes.ACC_PUBLIC != 0

  fun isPublic(field: FieldNode): Boolean = field.access and Opcodes.ACC_PUBLIC != 0

  fun isDeprecated(classNode: ClassNode): Boolean = classNode.access and Opcodes.ACC_DEPRECATED != 0

  fun isDeprecated(method: MethodNode): Boolean = method.access and Opcodes.ACC_DEPRECATED != 0

  fun isDeprecated(field: FieldNode): Boolean = field.access and Opcodes.ACC_DEPRECATED != 0

  fun isDefaultAccess(field: FieldNode): Boolean = !isPublic(field) && !isProtected(field) && !isPrivate(field)

  fun isDefaultAccess(method: MethodNode): Boolean = !isPublic(method) && !isProtected(method) && !isPrivate(method)

  fun isAbstract(method: MethodNode): Boolean = method.access and Opcodes.ACC_ABSTRACT != 0

  fun isProtected(field: FieldNode): Boolean = field.access and Opcodes.ACC_PROTECTED != 0

  fun isProtected(method: MethodNode): Boolean = method.access and Opcodes.ACC_PROTECTED != 0

  fun isStatic(method: MethodNode): Boolean = method.access and Opcodes.ACC_STATIC != 0

  fun isStatic(field: FieldNode): Boolean = field.access and Opcodes.ACC_STATIC != 0

  fun isSuperFlag(classNode: ClassNode): Boolean = classNode.access and Opcodes.ACC_SUPER != 0

  fun isSynthetic(method: MethodNode): Boolean = method.access and Opcodes.ACC_SYNTHETIC != 0

  fun isBridgeMethod(method: MethodNode): Boolean = method.access and Opcodes.ACC_BRIDGE != 0

  fun haveTheSamePackage(first: ClassNode, second: ClassNode): Boolean = extractPackage(first.name) == extractPackage(second.name)

  /**
   * Access Control
   * A class or interface C is accessible to a class or interface D if and only if either of the following is true:
   * C is public.
   * C and D are members of the same run-time package (§5.3).
   */
  fun isClassAccessibleToOtherClass(me: ClassNode, other: ClassNode): Boolean = isPublic(me) || haveTheSamePackage(me, other)

  private fun extractPackage(className: String): String = className.substringBeforeLast('/', "")

  /**
   * An instance method mC declared in class C overrides another instance method mA declared in
   * class A iff either mC is the same as mA, or all of the following are true:
   *  1) C is a subclass of A.
   *  2) mC has the same name and descriptor as mA.
   *  3) mC is not marked ACC_PRIVATE.
   *  4) One of the following is true:
   *      mA is marked ACC_PUBLIC; or is marked ACC_PROTECTED; or is marked neither ACC_PUBLIC nor ACC_PROTECTED
   *      nor ACC_PRIVATE and A belongs to the same run-time package as C.
   *      mC overrides a method m' (m' distinct from mC and mA) such that m' overrides mA.
   */
  fun overrides(firstOwner: ClassNode,
                firstMethod: MethodNode,
                secondOwner: ClassNode,
                secondMethod: MethodNode,
                ctx: VerificationContext): Boolean {
    if (firstOwner.name == secondOwner.name && firstMethod.name == secondMethod.name && firstMethod.desc == secondMethod.desc) {
      //the same
      return true
    }
    val isAccessible = isPublic(firstMethod) ||
        isProtected(firstMethod) ||
        (isDefaultAccess(firstMethod) && haveTheSamePackage(firstOwner, secondOwner))

    return isSubclassOf(firstOwner, secondOwner, ctx)
        && firstMethod.name == secondMethod.name && firstMethod.desc == secondMethod.desc
        && !isPrivate(firstMethod)
        && isAccessible
  }

  fun isSubclassOf(child: ClassNode, possibleParent: ClassNode, ctx: VerificationContext): Boolean {
    var current: ClassNode? = child
    while (current != null) {
      if (possibleParent.name == current.name) {
        return true
      }
      val superName = current.superName ?: return false
      current = ctx.findClassNode(superName)
    }
    return false
  }

}
