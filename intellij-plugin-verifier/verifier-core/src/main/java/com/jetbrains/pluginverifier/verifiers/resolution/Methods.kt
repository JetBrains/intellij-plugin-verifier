package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val LOG: Logger = LoggerFactory.getLogger(Method::class.java)

/**
 * Indicates a binary class name where packages are delimited by '/' instead of '.'.
 * This is in line with Java Language Specification.
 */
typealias BinaryClassName = String

const val JAVA_LANG_OBJECT: BinaryClassName = "java/lang/Object"

fun Method.isOverriding(anotherMethod: Method): Boolean =
  nonStaticAndNonFinal(anotherMethod)
    && sameName(this, anotherMethod)
    && sameParametersAndReturnType(this, anotherMethod)
    && sameOrBroaderVisibility(this, anotherMethod)

data class MethodInClass(val method: Method, val klass: ClassFile)

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

/**
 * Search for all methods in the parent hierarchy that the receiver overrides.
 * @return list of methods, sorted from bottom-to-top in the inheritance hierarchy.
 */
fun Method.searchParentOverrides(resolver: Resolver): List<MethodInClass> {
  return mutableListOf<MethodInClass>().apply {
    searchParentOverrides(resolver) { klass: ClassFile, method: Method ->
      when {
        klass.name == JAVA_LANG_OBJECT -> Unit
        else -> add(MethodInClass(method, klass))
      }
    }
  }
}

fun Method.searchParentOverrides(resolver: Resolver, matchHandler: (ClassFile, Method) -> Unit) {
  var superClassFqn = containingClassFile.superName ?: return
  while (true) {
    val superClass = when (val resolvedClass = resolver.resolveClass(superClassFqn)) {
      is ResolutionResult.Found<ClassNode> -> ClassFileAsm(resolvedClass.value, resolvedClass.fileOrigin)
      else -> return
    }
    findInSuperClass(superClass)?.let { overriddenMethod ->
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

fun isCallOfSuperMethod(callerMethod: Method, calledMethod: Method, instructionNode: AbstractInsnNode): Boolean {
  return callerMethod.name == calledMethod.name
    && callerMethod.descriptor == calledMethod.descriptor
    && instructionNode.opcode == Opcodes.INVOKESPECIAL
}

data class MethodDescriptor(private val methodName: String, private val descriptor: String)

fun Method.matches(method: Method): Boolean =
  MethodDescriptor(name, descriptor) == MethodDescriptor(method.name, method.descriptor)

fun MethodInsnNode.matches(method: Method): Boolean =
  MethodDescriptor(name, desc) == MethodDescriptor(method.name, method.descriptor)