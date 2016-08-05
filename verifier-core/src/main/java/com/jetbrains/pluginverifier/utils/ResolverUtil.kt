package com.jetbrains.pluginverifier.utils

import com.intellij.structure.impl.utils.StringUtil
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import java.util.function.Predicate

/**
 * @author Dennis.Ushakov
 */
object ResolverUtil {

  private fun findMethod(resolver: Resolver, className: String, methodName: String, methodDesc: String, ctx: VContext, childName: String): MethodLocation? {
    if (className.startsWith("[")) {
      throw RuntimeException("Method owner class must not be an array class")
    }

    val classFile = VerifierUtil.findClass(resolver, className, ctx)
    if (classFile == null) {
      if (!ctx.verifierOptions.isExternalClass(className)) {
        ctx.registerProblem(ClassNotFoundProblem(className), ProblemLocation.fromClass(childName)) //TODO: add a 'super' location
      }
      return null
    }

    return findMethod(resolver, classFile, methodName, methodDesc, ctx)
  }

  @Suppress("UNCHECKED_CAST")
  fun findMethod(resolver: Resolver, clazz: ClassNode, methodName: String, methodDesc: String, ctx: VContext): MethodLocation? {
    for (method in clazz.methods as List<MethodNode>) {
      if (methodName == method.name && methodDesc == method.desc) {
        return MethodLocation(clazz, method)
      }
    }

    if (clazz.superName != null) {
      val res = findMethod(resolver, clazz.superName, methodName, methodDesc, ctx, clazz.name)
      if (res != null) {
        return res
      }
    }

    for (anInterface in clazz.interfaces) {
      val res = findMethod(resolver, anInterface as String, methodName, methodDesc, ctx, clazz.name)
      if (res != null) {
        return res
      }
    }

    return null
  }

  private fun findField(resolver: Resolver, className: String, fieldName: String, fieldDescriptor: String, ctx: VContext, childName: String): FieldLocation? {
    if (className.startsWith("[")) {
      throw RuntimeException("Method owner class must not be an array class")
    }

    val classFile = VerifierUtil.findClass(resolver, className, ctx)
    if (classFile == null) {
      if (!ctx.verifierOptions.isExternalClass(className)) {
        ctx.registerProblem(ClassNotFoundProblem(className), ProblemLocation.fromClass(childName))
      }
      return null
    }

    return findField(resolver, classFile, fieldName, fieldDescriptor, ctx)
  }

  @Suppress("UNCHECKED_CAST")
  fun findField(resolver: Resolver, clazz: ClassNode, fieldName: String, fieldDescriptor: String, ctx: VContext): FieldLocation? {
    for (field in clazz.fields as List<FieldNode>) {
      if (StringUtil.equal(field.name, fieldName) && StringUtil.equal(field.desc, fieldDescriptor)) {
        return FieldLocation(clazz, field)
      }
    }

    //superinterfaces first
    for (anInterface in clazz.interfaces as List<String>) {
      val res = findField(resolver, anInterface, fieldName, fieldDescriptor, ctx, clazz.name)
      if (res != null) {
        return res
      }
    }

    //superclass second
    if (clazz.superName != null) {
      val res = findField(resolver, clazz.superName, fieldName, fieldDescriptor, ctx, clazz.name)
      if (res != null) {
        return res
      }
    }

    return null
  }

  fun collectUnresolvedClasses(resolver: Resolver, className: String, ctx: VContext): Set<String> {
    VerifierUtil.findClass(resolver, className, ctx) ?: return setOf(className)
    return ParentsVisitor(resolver, ctx).collectUnresolvedParents(className, Predicate { s -> false })
  }

  class FieldLocation internal constructor(val classNode: ClassNode, val fieldNode: FieldNode)

  class MethodLocation internal constructor(val classNode: ClassNode, val methodNode: MethodNode)
}
