package com.jetbrains.pluginverifier.utils

import com.google.common.base.Preconditions
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.warnings.Warning
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import org.slf4j.LoggerFactory

object VerifierUtil {

  private val LOG = LoggerFactory.getLogger(VerifierUtil::class.java)

  fun classExistsOrExternal(ctx: VContext, resolver: Resolver, className: String): Boolean {
    Preconditions.checkArgument(!className.startsWith("["), className)
    Preconditions.checkArgument(!className.endsWith(";"), className)

    return ctx.verifierOptions.isExternalClass(className) || resolver.containsClass(className)
  }

  fun classExistsOrExternal(ctx: VContext, potential: ClassNode, resolver: Resolver, descr: String): Boolean = descr == potential.name || classExistsOrExternal(ctx, resolver, descr)

  fun isInterface(classNode: ClassNode): Boolean = classNode.access and Opcodes.ACC_INTERFACE != 0

  fun findClass(resolver: Resolver, potential: ClassNode, className: String, ctx: VContext): ClassNode? {
    if (className == potential.name) {
      return potential
    }
    return findClass(resolver, className, ctx)
  }


  /**
   * Finds a class with the given name in the given resolver
   *
   * @param resolver  resolver to search in
   * @param className className in binary form
   * @param ctx       context to report a problem of missing class to
   * @return null if not found or exception occurs (in the last case 'failed to read' warning is reported)
   */
  fun findClass(resolver: Resolver, className: String, ctx: VContext): ClassNode? {
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

  fun isFinal(classNode: ClassNode): Boolean = classNode.access and Opcodes.ACC_FINAL != 0

  fun isFinal(superMethod: MethodNode): Boolean = superMethod.access and Opcodes.ACC_FINAL != 0

  fun isFinal(fieldNode: FieldNode): Boolean = fieldNode.access and Opcodes.ACC_FINAL != 0

  fun isAbstract(clazz: ClassNode): Boolean = clazz.access and Opcodes.ACC_ABSTRACT != 0

  fun isPrivate(method: MethodNode): Boolean = method.access and Opcodes.ACC_PRIVATE != 0

  fun isPrivate(field: FieldNode): Boolean = field.access and Opcodes.ACC_PRIVATE != 0

  private fun isPublic(method: MethodNode): Boolean = method.access and Opcodes.ACC_PUBLIC != 0

  private fun isPublic(field: FieldNode): Boolean = field.access and Opcodes.ACC_PUBLIC != 0

  fun isDefaultAccess(field: FieldNode): Boolean = !isPublic(field) && !isProtected(field) && !isPrivate(field)

  fun isDefaultAccess(method: MethodNode): Boolean = !isPublic(method) && !isProtected(method) && !isPrivate(method)

  fun isAbstract(method: MethodNode): Boolean = method.access and Opcodes.ACC_ABSTRACT != 0

  fun isProtected(field: FieldNode): Boolean = field.access and Opcodes.ACC_PROTECTED != 0

  fun isProtected(method: MethodNode): Boolean = method.access and Opcodes.ACC_PROTECTED != 0

  fun isStatic(method: MethodNode): Boolean = method.access and Opcodes.ACC_STATIC != 0

  fun isStatic(field: FieldNode): Boolean = field.access and Opcodes.ACC_STATIC != 0

  fun haveTheSamePackage(first: ClassNode, second: ClassNode): Boolean = extractPackage(first.name).equals(extractPackage(second.name), false)

  private fun extractPackage(className: String?): String? {
    if (className == null) return null
    val slash = className.lastIndexOf('/')
    if (slash == -1) return className
    return className.substring(0, slash)
  }

  fun isAncestor(child: ClassNode, possibleParent: ClassNode, resolver: Resolver, ctx: VContext): Boolean {
    var child1: ClassNode? = child
    while (child1 != null) {
      if (possibleParent.name.equals(child1.name, false)) {
        return true
      }
      val superName = child1.superName ?: return false
      child1 = findClass(resolver, superName, ctx)
    }
    return false
  }

  fun hasUnresolvedParentClass(clazz: String,
                               resolver: Resolver,
                               ctx: VContext): Boolean {
    findClass(resolver, clazz, ctx) ?: return true

    val unresolvedClasses = ResolverUtil.collectUnresolvedClasses(resolver, clazz, ctx)
    return !unresolvedClasses.isEmpty()
  }
}
