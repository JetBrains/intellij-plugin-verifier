package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.verifiers.isSubclassOrSelf
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val LOG: Logger = LoggerFactory.getLogger(Method::class.java)

/**
 * Indicates a binary class name where packages are delimited by '/' instead of '.'.
 * This is in line with the Java Language Specification.
 */
typealias BinaryClassName = String

/**
 * Indicates a fully qualified class name where packages are delimited by '.' (comma).
 */
typealias FullyQualifiedClassName = String

const val JAVA_LANG_OBJECT: BinaryClassName = "java/lang/Object"

/**
 * Checks if the receiver is overriding another method.
 *
 * @param possiblyParentMethod an override candidate method, possibly in parent class
 * @param resolver used to resolve covariant relationship between return types in the receiver and
 * the method in the argument
 */
fun Method.isOverriding(possiblyParentMethod: Method, resolver: Resolver): Boolean {
  return sameName(this, possiblyParentMethod)
    && nonStaticAndNonFinal(possiblyParentMethod)
    && sameOrBroaderVisibility(this, possiblyParentMethod)
    && sameParameters(this, possiblyParentMethod)
    && sameOrCovariantReturnType(this, possiblyParentMethod, resolver)
}

data class MethodInClass(val method: Method, val klass: ClassFile)

private fun nonStaticAndNonFinal(anotherMethod: Method) = !anotherMethod.isStatic && !anotherMethod.isFinal

private fun sameName(method: Method, anotherMethod: Method) = method.name == anotherMethod.name

private fun sameOrBroaderVisibility(method: Method, anotherMethod: Method): Boolean {
  return method.visibilityRating >= anotherMethod.visibilityRating
}

fun Method.hasSameOrigin(method: Method): Boolean =
  containingClassFile.classFileOrigin == method.containingClassFile.classFileOrigin

fun Method.hasDifferentOrigin(method: Method): Boolean = !hasSameOrigin(method)

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
    val overridableMethodInParent = findInSuperClass(superClass) { child, parent ->
      child.isOverriding(parent, resolver)
    }
    overridableMethodInParent?.let { overriddenMethod ->
        matchHandler(superClass, overriddenMethod)
      }
    superClassFqn = superClass.superName ?: return
  }
}

private fun Method.findInSuperClass(superClass: ClassFileAsm, isSuperClassMethod: (Method, Method) -> Boolean): Method? {
  val superClassMethodCandidates = superClass.methods.filter { superMethod ->
    isSuperClassMethod(this, superMethod)
  }.toList()
  return when {
    superClassMethodCandidates.isEmpty() -> null
    superClassMethodCandidates.size == 1 -> superClassMethodCandidates.first()
    else -> {
      LOG.debug("Too many candidates for discovering overridden method in superclass for ${this.name}")
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

fun Method.matches(method: Method): Boolean {
  return MethodDescriptor(name, descriptor) == MethodDescriptor(method.name, method.descriptor)
}

fun MethodInsnNode.matches(method: Method): Boolean =
  MethodDescriptor(name, desc) == MethodDescriptor(method.name, method.descriptor)

fun Method.returnType(): BinaryClassName {
  val returnType: Type = Type.getReturnType(this.descriptor)
  return returnType.className.toBinaryClassName()
}

fun sameOrCovariantReturnType(method: Method, possiblyParentMethod: Method, resolver: Resolver): Boolean {
  val returnType: BinaryClassName = method.returnType()
  val possibleParentReturnType: BinaryClassName = possiblyParentMethod.returnType()

  return resolver.isSubclassOrSelf(returnType, possibleParentReturnType)
}

fun sameParameters(method: Method, anotherMethod: Method): Boolean {
  val argTypes: Array<Type> = Type.getArgumentTypes(method.descriptor)
  val anotherArgTypes: Array<Type> = Type.getArgumentTypes(anotherMethod.descriptor)

  if (argTypes.size != anotherArgTypes.size) {
    return false
  }

  return argTypes.indices.all { index ->
    argTypes[index] == anotherArgTypes[index]
  }
}

fun FullyQualifiedClassName.toBinaryClassName(): BinaryClassName = replace('.', '/')
fun BinaryClassName.toFullyQualifiedClassName(): FullyQualifiedClassName = replace('/', '.')