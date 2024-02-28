package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import org.objectweb.asm.tree.ClassNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val LOG: Logger = LoggerFactory.getLogger(Method::class.java)

fun Method.isOverriding(anotherMethod: Method): Boolean =
  nonStaticAndNonFinal(anotherMethod)
    && sameName(this, anotherMethod)
    && sameParametersAndReturnType(this, anotherMethod)
    && sameOrBroaderVisibility(this, anotherMethod)

private fun nonStaticAndNonFinal(anotherMethod: Method) = !anotherMethod.isStatic && !anotherMethod.isFinal

private fun sameName(method: Method, anotherMethod: Method) = method.name == anotherMethod.name

private fun sameParametersAndReturnType(method: Method, anotherMethod: Method) =
  method.descriptor == anotherMethod.descriptor

private fun sameOrBroaderVisibility(method: Method, anotherMethod: Method): Boolean {
  return method.visibilityRating >= anotherMethod.visibilityRating
}

private val Method.visibilityRating: Int
  get() = when {
    isPublic -> 3
    isProtected -> 2
    isPackagePrivate -> 1
    isPrivate -> 0
    else -> -1
  }

fun Method.search(resolver: Resolver, matchHandler: (ClassFile, Method) -> Unit) {
  var superClassFqn = containingClassFile.superName ?: return
  while (true) {
    val superClass = when(val resolvedClass = resolver.resolveClass(superClassFqn)) {
      is ResolutionResult.Found<ClassNode> -> ClassFileAsm(resolvedClass.value, resolvedClass.fileOrigin)
      else -> return
    }
    findInSuperClass(superClass)?.let {overriddenMethod ->
      matchHandler(superClass, overriddenMethod)
    }
    superClassFqn = superClass.superName ?: return
  }
}

private fun Method.findInSuperClass(superClass: ClassFileAsm): Method? {
  val superClassMethodCandidates = superClass.methods.filter { superMethod ->
    isOverriding(superMethod)
  }.toList()
  return when {
    superClassMethodCandidates.isEmpty() -> null
    superClassMethodCandidates.size == 1 -> superClassMethodCandidates.first()
    else -> {
      LOG.warn("Too many candidates for discovering overridden method in superclass for ${this.name}")
      null
    }
  }
}
