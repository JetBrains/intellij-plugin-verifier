package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.results.access.AccessType
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*

@Suppress("UNCHECKED_CAST")
fun MethodNode.getParameterNames(): List<String> {
  val arguments = Type.getArgumentTypes(desc)
  val argumentsNumber = arguments.size
  val offset = if (this.isStatic()) 0 else 1
  var parameterNames: List<String> = emptyList()
  if (localVariables != null) {
    parameterNames = (localVariables as List<LocalVariableNode>).map { it.name }.drop(offset).take(argumentsNumber)
  }
  if (parameterNames.size != argumentsNumber) {
    parameterNames = (0..argumentsNumber - 1).map { "arg$it" }
  }
  return parameterNames
}

/**
 * Peels an internal JVM descriptor of a type.
 *
 * For arrays returns the innermost type.
 *
 * For primitive types returns `null`.
 *
 * Examples:
 * - `Lcom/example/Example;` -> `com/example/Example`
 * - `[[[Lcom/example/Example;` -> `com/example/Example`
 * - `I`, `D`, `B` -> `null`
 * - `[[I` -> `null`
 */
fun String.extractClassNameFromDescr(): String? {
  //prepare array name
  val elementType = trimStart('[')

  if (elementType.isPrimitiveType()) {
    return null
  }

  if (elementType.startsWith("L") && elementType.endsWith(";")) {
    return elementType.substring(1, elementType.length - 1)
  }

  return elementType
}

private fun String.isPrimitiveType(): Boolean = length == 1 && first() in "ZIJBFSDC"

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
        && methodNode.isVararg()
        && methodNode.isNative()

fun Int.getAccessType(): AccessType = when {
  this and Opcodes.ACC_PUBLIC != 0 -> AccessType.PUBLIC
  this and Opcodes.ACC_PRIVATE != 0 -> AccessType.PRIVATE
  this and Opcodes.ACC_PROTECTED != 0 -> AccessType.PROTECTED
  else -> AccessType.PACKAGE_PRIVATE
}

fun MethodNode.isVararg(): Boolean = access and Opcodes.ACC_VARARGS != 0

fun MethodNode.isNative(): Boolean = access and Opcodes.ACC_NATIVE != 0

fun ClassNode.isFinal(): Boolean = access and Opcodes.ACC_FINAL != 0

fun MethodNode.isFinal(): Boolean = access and Opcodes.ACC_FINAL != 0

fun FieldNode.isFinal(): Boolean = access and Opcodes.ACC_FINAL != 0

fun ClassNode.isInterface(): Boolean = access and Opcodes.ACC_INTERFACE != 0

fun ClassNode.isAbstract(): Boolean = access and Opcodes.ACC_ABSTRACT != 0

fun MethodNode.isPrivate(): Boolean = access and Opcodes.ACC_PRIVATE != 0

fun FieldNode.isPrivate(): Boolean = access and Opcodes.ACC_PRIVATE != 0

fun ClassNode.isPublic(): Boolean = access and Opcodes.ACC_PUBLIC != 0

fun MethodNode.isPublic(): Boolean = access and Opcodes.ACC_PUBLIC != 0

fun FieldNode.isPublic(): Boolean = access and Opcodes.ACC_PUBLIC != 0

fun ClassNode.isDeprecated(): Boolean = access and Opcodes.ACC_DEPRECATED != 0

fun MethodNode.isDeprecated(): Boolean = access and Opcodes.ACC_DEPRECATED != 0

fun FieldNode.isDeprecated(): Boolean = access and Opcodes.ACC_DEPRECATED != 0

fun FieldNode.isDefaultAccess(): Boolean = !isPublic() && !this.isProtected() && !isPrivate()

fun MethodNode.isDefaultAccess(): Boolean = !isPublic() && !this.isProtected() && !isPrivate()

fun MethodNode.isAbstract(): Boolean = access and Opcodes.ACC_ABSTRACT != 0

fun MethodNode.isConstructor(): Boolean = name == "<init>"

fun MethodNode.isClassInitializer(): Boolean = name == "<clinit>"

fun FieldNode.isProtected(): Boolean = access and Opcodes.ACC_PROTECTED != 0

fun MethodNode.isProtected(): Boolean = access and Opcodes.ACC_PROTECTED != 0

fun MethodNode.isStatic(): Boolean = access and Opcodes.ACC_STATIC != 0

fun FieldNode.isStatic(): Boolean = access and Opcodes.ACC_STATIC != 0

fun ClassNode.isSuperFlag(): Boolean = access and Opcodes.ACC_SUPER != 0

fun MethodNode.isSynthetic(): Boolean = access and Opcodes.ACC_SYNTHETIC != 0

fun MethodNode.isBridgeMethod(): Boolean = access and Opcodes.ACC_BRIDGE != 0

fun haveTheSamePackage(first: ClassNode, second: ClassNode): Boolean = extractPackage(first.name) == extractPackage(second.name)

@Suppress("UNCHECKED_CAST")
fun ClassNode.getInvisibleAnnotations() = (invisibleAnnotations as? List<AnnotationNode>).orEmpty()

@Suppress("UNCHECKED_CAST")
fun ClassNode.getVisibleAnnotations() = (visibleAnnotations as? List<AnnotationNode>).orEmpty()

/**
 * Access Control
 * A class or interface C is accessible to a class or interface D if and only if either of the following is true:
 * C is public.
 * C and D are members of the same run-time package (§5.3).
 */
fun isClassAccessibleToOtherClass(me: ClassNode, other: ClassNode): Boolean = me.isPublic() || haveTheSamePackage(me, other)

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
  val isAccessible = firstMethod.isPublic() ||
      firstMethod.isProtected() ||
      (firstMethod.isDefaultAccess() && haveTheSamePackage(firstOwner, secondOwner))

  return ctx.isSubclassOf(firstOwner, secondOwner)
      && firstMethod.name == secondMethod.name && firstMethod.desc == secondMethod.desc
      && !firstMethod.isPrivate()
      && isAccessible
}