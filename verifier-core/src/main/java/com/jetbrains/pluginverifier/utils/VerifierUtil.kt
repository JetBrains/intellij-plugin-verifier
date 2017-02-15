package com.jetbrains.pluginverifier.utils

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.AccessType
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.problems.IllegalClassAccessProblem
import com.jetbrains.pluginverifier.warnings.Warning
import org.jetbrains.intellij.plugins.internal.asm.Opcodes
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.FieldNode
import org.jetbrains.intellij.plugins.internal.asm.tree.MethodNode
import org.slf4j.LoggerFactory

sealed class ClsResolution() {
  object NotFound : ClsResolution()
  object ExternalClass : ClsResolution()
  class IllegalAccess(val accessType: AccessType) : ClsResolution()
  class Found(val node: ClassNode) : ClsResolution()
}

fun VContext.registerMissingClass(clsName: String, problemLocation: ProblemLocation) = this.registerProblem(ClassNotFoundProblem(clsName), problemLocation)

object VerifierUtil {

  private val LOG = LoggerFactory.getLogger(VerifierUtil::class.java)

  /**
   * To resolve an unresolved symbolic reference from D to a class or interface C denoted by N, the following steps are performed:
   * ...<JVM-related stuff>...
   *  3) Finally, access permissions to C are checked.
   *  If C is not accessible (§5.4.4) to D, class or interface resolution throws an IllegalAccessError.
   */
  private fun resolveClass(resolver: Resolver, className: String, lookup: ClassNode, ctx: VContext): ClsResolution {
    if (ctx.verifierOptions.isExternalClass(className)) {
      return ClsResolution.ExternalClass
    }
    val node = findClassNode(resolver, className, ctx)
    if (node != null) {
      return if (isClassAccessibleToOtherClass(node, lookup)) {
        ClsResolution.Found(node)
      } else {
        ClsResolution.IllegalAccess(getAccessType(node.access))
      }
    }
    return ClsResolution.NotFound
  }

  fun resolveClassOrProblem(resolver: Resolver,
                            className: String,
                            lookup: ClassNode,
                            ctx: VContext,
                            lookupLocation: (() -> ProblemLocation)): ClassNode? {
    val resolveClass = resolveClass(resolver, className, lookup, ctx)
    return when (resolveClass) {
      ClsResolution.NotFound -> {
        ctx.registerMissingClass(className, lookupLocation())
        null
      }
      ClsResolution.ExternalClass -> null
      is ClsResolution.IllegalAccess -> {
        ctx.registerProblem(IllegalClassAccessProblem(className, resolveClass.accessType), lookupLocation())
        null
      }
      is ClsResolution.Found -> resolveClass.node
    }
  }


  fun checkClassExistsOrExternal(resolver: Resolver, className: String, ctx: VContext, registerMissing: (() -> ProblemLocation)) {
    if (!ctx.verifierOptions.isExternalClass(className) && !resolver.containsClass(className)) {
      ctx.registerProblem(ClassNotFoundProblem(className), registerMissing())
    }
  }

  /**
   * Finds a class with the given name in the given resolver
   *
   * @param resolver  resolver to search in
   * @param className className in binary form
   * @param ctx       context to report a problem of missing class to
   * @return null if not found or exception occurs (in the last case 'failed to read' warning is reported)
   */
  private fun findClassNode(resolver: Resolver, className: String, ctx: VContext): ClassNode? {
    try {
      return resolver.findClass(className)
    } catch (e: Exception) {
      LOG.debug("Unable to read a class file $className", e)
      ctx.registerWarning(Warning("Unable to read a class $className using ASM (<a href=\"http://asm.ow2.org\"></a>). Probably it has invalid class-file. Try to recompile the plugin"))
      return null
    }

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
                resolver: Resolver,
                ctx: VContext): Boolean {
    if (firstOwner.name == secondOwner.name && firstMethod.name == secondMethod.name && firstMethod.desc == secondMethod.desc) {
      //the same
      return true
    }
    val isAccessible = isPublic(firstMethod) ||
        isProtected(firstMethod) ||
        (isDefaultAccess(firstMethod) && haveTheSamePackage(firstOwner, secondOwner))

    return isSubclassOf(firstOwner, secondOwner, resolver, ctx)
        && firstMethod.name == secondMethod.name && firstMethod.desc == secondMethod.desc
        && !isPrivate(firstMethod)
        && isAccessible
  }

  fun isSubclassOf(child: ClassNode, possibleParent: ClassNode, resolver: Resolver, ctx: VContext): Boolean {
    var current: ClassNode? = child
    while (current != null) {
      if (possibleParent.name == current.name) {
        return true
      }
      val superName = current.superName ?: return false
      current = findClassNode(resolver, superName, ctx)
    }
    return false
  }

  fun fromMethod(hostClass: String, method: MethodNode) = ProblemLocation.fromMethod(hostClass, method.name, method.desc)

}
