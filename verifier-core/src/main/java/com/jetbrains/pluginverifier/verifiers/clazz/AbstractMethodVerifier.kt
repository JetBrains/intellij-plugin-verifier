package com.jetbrains.pluginverifier.verifiers.clazz

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.location.MethodLocation
import com.jetbrains.pluginverifier.problems.MethodNotImplementedProblem
import com.jetbrains.pluginverifier.utils.VerificationContext
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.MethodNode

/**
 * @author Sergey Patrikeev
 */
class AbstractMethodVerifier : ClassVerifier {
  override fun verify(clazz: ClassNode, resolver: Resolver, ctx: VerificationContext) {
    if (VerifierUtil.isAbstract(clazz) || VerifierUtil.isInterface(clazz)) return

    val abstractMethods = hashMapOf<Method, MethodLocation>()
    val implementedMethods = hashMapOf<Method, MethodLocation>()
    traverseTree(clazz, resolver, ctx, hashSetOf(), abstractMethods, implementedMethods)

    val classLocation = ctx.fromClass(clazz)
    (abstractMethods.keys - implementedMethods.keys).forEach { method ->
      val abstractMethod = abstractMethods[method]!!
      ctx.registerProblem(MethodNotImplementedProblem(abstractMethod, classLocation))
    }
  }

  private data class Method(val name: String, val descriptor: String)

  @Suppress("UNCHECKED_CAST")
  private fun traverseTree(clazz: ClassNode,
                           resolver: Resolver,
                           ctx: VerificationContext,
                           visitedClasses: MutableSet<String>,
                           abstractMethods: MutableMap<Method, MethodLocation>,
                           implementedMethods: MutableMap<Method, MethodLocation>) {
    (clazz.methods as List<MethodNode>).forEach {
      if (!VerifierUtil.isPrivate(it) && !VerifierUtil.isStatic(it)) {
        val methodLocation = ctx.fromMethod(clazz, it)
        if (VerifierUtil.isAbstract(it)) {
          abstractMethods.put(Method(it.name, it.desc), methodLocation)
        } else {
          implementedMethods.put(Method(it.name, it.desc), methodLocation)
        }
      }
    }

    visitedClasses.add(clazz.name)

    val superName: String = clazz.superName ?: "java/lang/Object"

    (listOf(superName) + (clazz.interfaces as List<String>)).forEach { clsName ->
      if (!visitedClasses.contains(clsName)) {
        val node = VerifierUtil.resolveClassOrProblem(resolver, clsName, clazz, ctx, { ctx.fromClass(clazz) })
        if (node != null) {
          traverseTree(node, resolver, ctx, visitedClasses, abstractMethods, implementedMethods)
        }
      }
    }

  }
}