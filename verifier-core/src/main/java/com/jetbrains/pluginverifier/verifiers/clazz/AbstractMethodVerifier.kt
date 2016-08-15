package com.jetbrains.pluginverifier.verifiers.clazz

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ClassLocation
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.MethodNotImplementedProblem
import com.jetbrains.pluginverifier.reference.MethodReference
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.util.*

/**
 * @author Sergey Patrikeev
 */
class AbstractMethodVerifier : ClassVerifier {
  override fun verify(clazz: ClassNode, resolver: Resolver, ctx: VContext) {
    if (VerifierUtil.isAbstract(clazz) || VerifierUtil.isInterface(clazz)) return

    val abstractMethods = hashMapOf<Method, String>()
    val implementedMethods = hashMapOf<Method, String>()
    traverseTree(clazz, resolver, ctx, hashSetOf(), abstractMethods, implementedMethods)

    (abstractMethods.keys - implementedMethods.keys).forEach {
      ctx.registerProblem(MethodNotImplementedProblem(MethodReference.from(abstractMethods[it]!!, it.name, it.descriptor)), ClassLocation(clazz.name))
    }
  }

  private data class Method(val name: String, val descriptor: String)

  @Suppress("UNCHECKED_CAST")
  private fun traverseTree(clazz: ClassNode,
                           resolver: Resolver,
                           ctx: VContext,
                           visitedClasses: MutableSet<String>,
                           abstractMethods: HashMap<Method, String>,
                           implementedMethods: HashMap<Method, String>) {
    (clazz.methods as List<MethodNode>).forEach {
      if (!VerifierUtil.isPrivate(it) && !VerifierUtil.isStatic(it)) {
        if (VerifierUtil.isAbstract(it)) {
          abstractMethods.put(Method(it.name, it.desc), clazz.name)
        } else {
          implementedMethods.put(Method(it.name, it.desc), clazz.name)
        }
      }
    }

    visitedClasses.add(clazz.name)

    val superName: String = clazz.superName ?: "java/lang/Object"

    (listOf(superName) + (clazz.interfaces as List<String>)).forEach { clsName ->
      if (!visitedClasses.contains(clsName)) {
        val node = VerifierUtil.resolveClassOrProblem(resolver, clsName, clazz, ctx, { ProblemLocation.fromClass(clazz.name) })
        if (node != null) {
          traverseTree(node, resolver, ctx, visitedClasses, abstractMethods, implementedMethods)
        }
      }
    }

  }
}