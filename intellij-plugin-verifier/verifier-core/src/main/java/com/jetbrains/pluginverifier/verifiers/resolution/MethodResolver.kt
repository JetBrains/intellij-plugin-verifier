/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.problems.*
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.hierarchy.ClassHierarchyBuilder
import com.jetbrains.pluginverifier.verifiers.isSubclassOf
import java.util.*

/**
 * Utility class that implements methods resolution strategies,
 * as described in the JVM specification (§5.4.3.3, §5.4.3.4).
 */
class MethodResolver {

  fun resolveMethod(
    ownerClass: ClassFile,
    methodReference: MethodReference,
    instruction: Instruction,
    callerMethod: Method,
    context: VerificationContext
  ): Method? =
    when (val resolutionResult = MethodResolveImpl(methodReference, instruction, callerMethod, context).resolveMethod(ownerClass)) {
      MethodResolutionResult.Abort -> null
      is MethodResolutionResult.NotFound -> {
        registerMethodNotFoundProblem(ownerClass, context, methodReference, instruction, callerMethod)
        null
      }
      is MethodResolutionResult.Found -> {
        checkMethodIsAccessible(resolutionResult.method, context, methodReference, callerMethod, instruction)
        resolutionResult.method
      }
    }

  fun lookupSpecialMethod(
    ownerClass: ClassFile,
    methodReference: MethodReference,
    instruction: Instruction,
    callerLocation: Method,
    context: VerificationContext,
    resolvedMethod: Method
  ): Pair<Int, Method>? = MethodResolveImpl(
    methodReference,
    instruction,
    callerLocation,
    context
  ).lookupSpecialMethod(ownerClass, resolvedMethod)

  private fun registerMethodNotFoundProblem(
    ownerClass: ClassFile,
    context: VerificationContext,
    methodReference: MethodReference,
    instruction: Instruction,
    callerMethod: Method
  ) {
    val methodOwnerHierarchy = ClassHierarchyBuilder(context).buildClassHierarchy(ownerClass)
    context.problemRegistrar.registerProblem(
      MethodNotFoundProblem(
        methodReference,
        callerMethod.location,
        instruction,
        methodOwnerHierarchy
      )
    )
  }

  /**
   * A field or method R is accessible to a class or interface D if and only if any of the following is true:
   * - R is public.
   * - R is protected and is declared in a class C, and D is either a subclass of C or C itself.
   * Furthermore, if R is not static, then the symbolic reference to R must contain a symbolic reference
   * to a class T, such that T is either a subclass of D, a superclass of D, or D itself.
   * - R is either protected or has default access (that is, neither public nor protected nor private),
   * and is declared by a class in the same run-time package as D.
   * - R is private and is declared in D.
   */
  private fun checkMethodIsAccessible(
    resolvedMethod: Method,
    context: VerificationContext,
    methodReference: MethodReference,
    callerMethod: Method,
    instruction: Instruction
  ) {
    val accessProblem = detectAccessProblem(resolvedMethod, callerMethod, context)
    if (accessProblem != null) {
      context.problemRegistrar.registerProblem(
        IllegalMethodAccessProblem(
          methodReference,
          resolvedMethod.location,
          accessProblem,
          callerMethod.location,
          instruction
        )
      )
    }
  }

}

private sealed class MethodResolutionResult {
  object Abort : MethodResolutionResult()

  object NotFound : MethodResolutionResult()

  data class Found(val method: Method) : MethodResolutionResult()
}

private class MethodResolveImpl(
  private val methodReference: MethodReference,
  private val instruction: Instruction,
  private val callerMethod: Method,
  private val context: VerificationContext
) {

  private val methodName = methodReference.methodName

  private val methodDescriptor = methodReference.methodDescriptor

  fun resolveMethod(ownerClass: ClassFile): MethodResolutionResult =
    when (instruction) {
      Instruction.INVOKE_VIRTUAL -> resolveClassMethod(ownerClass)
      Instruction.INVOKE_INTERFACE -> resolveInterfaceMethod(ownerClass)
      Instruction.INVOKE_STATIC -> resolveClassMethod(ownerClass)
      Instruction.INVOKE_SPECIAL -> if (ownerClass.isInterface) {
        resolveInterfaceMethod(ownerClass)
      } else {
        resolveClassMethod(ownerClass)
      }
      else -> throw IllegalArgumentException()
    }

  /**
   * To resolve an unresolved symbolic reference from D to an interface method in an interface C,
   * the symbolic reference to C given by the interface method reference is first resolved (§5.4.3.1).
   *
   * Therefore, any exception that can be thrown as a result of failure of resolution of an interface
   * reference can be thrown as a result of failure of interface method resolution.
   *
   * If the reference to C can be successfully resolved, exceptions relating to the resolution of the
   * interface method reference itself can be thrown.
   */
  private fun resolveInterfaceMethod(interfaceFile: ClassFile): MethodResolutionResult {
    /*
    1) If C is not an interface, interface method resolution throws an IncompatibleClassChangeError.
     */
    if (!interfaceFile.isInterface) {
      context.problemRegistrar.registerProblem(InvokeInterfaceMethodOnClassProblem(methodReference, callerMethod.location, instruction))
      return MethodResolutionResult.Abort
    }

    /*
    2) Otherwise, if C declares a method with the name and descriptor specified by
    the interface method reference, method lookup succeeds.
    */
    val matching = interfaceFile.methods.firstOrNull { it.name == methodName && it.descriptor == methodDescriptor }
    if (matching != null) {
      return MethodResolutionResult.Found(matching)
    }

    /*
    3) Otherwise, if the class Object declares a method with the name and descriptor specified by the
    interface method reference, which has its ACC_PUBLIC flag set and does not have its ACC_STATIC flag set,
    method lookup succeeds.
    */
    val objectClass = context.classResolver.resolveClassChecked("java/lang/Object", interfaceFile, context)
      ?: return MethodResolutionResult.Abort
    val objectMethod = objectClass.methods.firstOrNull { it.name == methodName && it.descriptor == methodDescriptor && it.isPublic && !it.isStatic }
    if (objectMethod != null) {
      return MethodResolutionResult.Found(objectMethod)
    }

    /*
    4) Otherwise, if the maximally-specific superinterface methods (§5.4.3.3) of C for the name
    and descriptor specified by the method reference include exactly one method that does not
    have its ACC_ABSTRACT flag set, then this method is chosen and method lookup succeeds.
     */
    val maximallySpecificSuperInterfaceMethods = getMaximallySpecificSuperInterfaceMethods(interfaceFile)
      ?: return MethodResolutionResult.Abort
    val single = maximallySpecificSuperInterfaceMethods.singleOrNull { it.name == methodName && it.descriptor == methodDescriptor && !it.isAbstract }
    if (single != null) {
      return MethodResolutionResult.Found(single)
    }

    /*
    5) Otherwise, if any superinterface of C declares a method with the name and descriptor specified by the method
    reference that has neither its ACC_PRIVATE flag nor its ACC_STATIC flag set, one of these is arbitrarily chosen and method lookup succeeds.
     */
    val matchingMethods = getSuperInterfaceMethods(interfaceFile) { it.name == methodName && it.descriptor == methodDescriptor && !it.isPrivate && !it.isStatic }
      ?: return MethodResolutionResult.Abort
    if (matchingMethods.isNotEmpty()) {
      return MethodResolutionResult.Found(matchingMethods.first())
    }

    /*
    6) Otherwise, method lookup fails.
     */
    return MethodResolutionResult.NotFound
  }

  /**
   * A maximally-specific superinterface method of a class or interface C for a particular method name
   * and descriptor is any method for which all of the following are true:
   *
   * - The method is declared in a superinterface (direct or indirect) of C.
   * - The method is declared with the specified name and descriptor.
   * - The method has neither its ACC_PRIVATE flag nor its ACC_STATIC flag set.
   * - Where the method is declared in interface I, there exists no other maximally-specific superinterface
   * method of C with the specified name and descriptor that is declared in a sub-interface of I.
   */
  private fun getMaximallySpecificSuperInterfaceMethods(start: ClassFile): List<Method>? {
    val predicate: (Method) -> Boolean = { it.name == methodName && it.descriptor == methodDescriptor && !it.isPrivate && !it.isStatic }
    val allMatching = getSuperInterfaceMethods(start, predicate) ?: return null
    return allMatching.filter { method ->
      allMatching.none { otherMethod ->
        otherMethod.containingClassFile.name != method.containingClassFile.name
          && context.classResolver.isSubclassOf(otherMethod.containingClassFile, method.containingClassFile.name)
      }
    }
  }

  /**
   * @return all direct and indirect super-interface methods matching the given predicate
   */
  private fun getSuperInterfaceMethods(start: ClassFile, predicate: (Method) -> Boolean): List<Method>? {
    //breadth-first-search
    val queue: Queue<ClassFile> = LinkedList<ClassFile>()
    val visited = hashSetOf<String>()
    val result = arrayListOf<Method>()
    queue.add(start)
    visited.add(start.name)
    while (!queue.isEmpty()) {
      val cur = queue.remove()
      cur.methods.filterTo(result, predicate)

      cur.interfaces.forEach {
        if (it !in visited) {
          val resolveClass = context.classResolver.resolveClassChecked(it, cur, context)
            ?: return null
          visited.add(it)
          queue.add(resolveClass)
        }
      }

      val superName = cur.superName
      if (superName != null) {
        if (superName !in visited) {
          val resolvedSuper = context.classResolver.resolveClassChecked(superName, cur, context)
            ?: return null
          visited.add(superName)
          queue.add(resolvedSuper)
        }
      }
    }
    return result
  }


  /**
   * To resolve an unresolved symbolic reference from D to a method in a class C,
   * the symbolic reference to C given by the method reference is first resolved (§5.4.3.1).
   *
   * Therefore, any exception that can be thrown as a result of failure of resolution of
   * a class reference can be thrown as a result of failure of method resolution.
   *
   * If the reference to C can be successfully resolved, exceptions relating to the resolution of the method reference itself can be thrown.
   */
  private fun resolveClassMethod(classFile: ClassFile): MethodResolutionResult {
    /*
      1) If C is an interface, method resolution throws an IncompatibleClassChangeError.
    */
    if (classFile.isInterface) {
      /*
      This additional if-condition ensures that we are not trying to resolve a static interface method at the moment.
      It is necessary because actually the JVM 8 spec chooses resolve-class-resolution algorithm unconditionally for resolving
      the static methods of both classes and interface.
      */
      if (instruction != Instruction.INVOKE_STATIC) {
        context.problemRegistrar.registerProblem(InvokeClassMethodOnInterfaceProblem(methodReference, callerMethod.location, instruction))
        return MethodResolutionResult.Abort
      }
    }

    /*
      2) Otherwise, method resolution attempts to locate the referenced method in C and its superclasses:
    */
    when (val lookupResult2 = resolveClassMethodStep2(classFile)) {
      MethodResolutionResult.Abort -> return lookupResult2
      is MethodResolutionResult.Found -> return lookupResult2
      MethodResolutionResult.NotFound -> Unit
    }

    /*
      3) Otherwise, method resolution attempts to locate the referenced method in the superinterfaces of the specified class C:
    */
    when (val lookupResult3 = resolveClassMethodStep3(classFile)) {
      MethodResolutionResult.Abort -> return lookupResult3
      is MethodResolutionResult.Found -> return lookupResult3
      MethodResolutionResult.NotFound -> Unit
    }

    return MethodResolutionResult.NotFound
  }

  /**
   * A method is signature polymorphic if all of the following are true:
   * - It is declared in the java.lang.invoke.MethodHandle class.
   *   or it is declared in the java.lang.invoke.VarHandle class.
   * - It has a single formal parameter of type Object[].
   * - It has a return type of Object.
   * - It has the ACC_VARARGS and ACC_NATIVE flags set.
   *
   * In Java SE 8, the only signature polymorphic methods are the invoke and invokeExact methods of the class java.lang.invoke.MethodHandle.
   */
  private fun isSignaturePolymorphic(methodNode: Method): Boolean =
    ("java/lang/invoke/MethodHandle" == methodNode.containingClassFile.name
      || "java/lang/invoke/VarHandle" == methodNode.containingClassFile.name)
      && ("([Ljava/lang/Object;)Ljava/lang/Object;" == methodNode.descriptor
      || "([Ljava/lang/Object;)Z" == methodNode.descriptor)
      && methodNode.isVararg
      && methodNode.isNative


  private fun resolveClassMethodStep2(currentClass: ClassFile): MethodResolutionResult {
    /*
    2.1) If C declares exactly one method with the name specified by the method reference,
      and the declaration is a signature polymorphic method (§2.9), then method lookup succeeds.

      All the class names mentioned in the descriptor are resolved (§5.4.3.1).

      The resolved method is the signature polymorphic method declaration. It is not necessary for C to declare
      a method with the descriptor specified by the method reference.
    */
    val methods = currentClass.methods

    val matchByName = methods.firstOrNull { it.name == methodName }
    if (matchByName != null && isSignaturePolymorphic(matchByName) && methods.count { it.name == methodName } == 1) {
      return MethodResolutionResult.Found(matchByName)
    }

    /*
    2.2) Otherwise, if C declares a method with the name and descriptor
    specified by the method reference, method lookup succeeds.
     */
    val matching = methods.find { methodName == it.name && methodDescriptor == it.descriptor }
    if (matching != null) {
      return MethodResolutionResult.Found(matching)
    }

    /*
    2.3) Otherwise, if C has a superclass, step 2 of method resolution is recursively invoked
    on the direct superclass of C.
     */
    val superName = currentClass.superName
    if (superName != null) {
      val resolvedSuper = context.classResolver.resolveClassChecked(superName, currentClass, context)
        ?: return MethodResolutionResult.Abort
      when (val lookupResult = resolveClassMethodStep2(resolvedSuper)) {
        is MethodResolutionResult.Found -> return lookupResult
        MethodResolutionResult.Abort -> return MethodResolutionResult.Abort
        MethodResolutionResult.NotFound -> Unit
      }
    }

    return MethodResolutionResult.NotFound
  }

  private fun resolveClassMethodStep3(currentClass: ClassFile): MethodResolutionResult {

    /*
      3.1) If the maximally-specific superinterface methods of C for the name and descriptor specified
    by the method reference include exactly one method that does not have its ACC_ABSTRACT
    flag set, then this method is chosen and method lookup succeeds.
     */
    val maximallySpecificSuperInterfaceMethods = getMaximallySpecificSuperInterfaceMethods(currentClass)
      ?: return MethodResolutionResult.Abort
    val single = maximallySpecificSuperInterfaceMethods.singleOrNull { it.name == methodName && it.descriptor == methodDescriptor && !it.isAbstract }
    if (single != null) {
      return MethodResolutionResult.Found(single)
    }

    /*
      3.2) Otherwise, if any superinterface of C declares a method with the name and descriptor specified
    by the method reference that has neither its ACC_PRIVATE flag nor its ACC_STATIC
    flag set, one of these is arbitrarily chosen and method lookup succeeds.
     */
    val matchingMethods = getSuperInterfaceMethods(currentClass) { it.name == methodName && it.descriptor == methodDescriptor && !it.isPrivate && !it.isStatic }
      ?: return MethodResolutionResult.Abort
    if (matchingMethods.isNotEmpty()) {
      return MethodResolutionResult.Found(matchingMethods.first())
    }

    /*
    3.3) Otherwise, method lookup fails.
     */
    return MethodResolutionResult.NotFound
  }

  /**
   * Resolves actual method to be invoked on 'invokespecial' instruction and the serial number of the successful lookup step.
   */
  fun lookupSpecialMethod(classRef: ClassFile, resolvedMethod: Method): Pair<Int, Method>? {
    /*
      1) If C contains a declaration for an instance method with the same name and descriptor as the resolved method,
      then it is the method to be invoked .
    */
    val matching = classRef.methods.find { it.name == resolvedMethod.name && it.descriptor == resolvedMethod.descriptor }
    if (matching != null) {
      return 1 to matching
    }

    /*
      2) Otherwise, if C is a class and has a superclass, a search for a declaration of an instance method with the same name
        and descriptor as the resolved method is performed, starting with the direct superclass of C and continuing with the
        direct superclass of that class, and so forth, until a match is found or no further superclasses exist.
        If a match is found, then it is the method to be invoked.
    */
    if (!classRef.isInterface && classRef.superName != null) {
      var current: ClassFile = context.classResolver.resolveClassChecked(classRef.superName!!, classRef, context)
        ?: return null
      while (true) {
        val match = current.methods.find { it.name == resolvedMethod.name && it.descriptor == resolvedMethod.descriptor }
        if (match != null) {
          return 2 to match
        }

        val superName = current.superName
        superName ?: break
        current = context.classResolver.resolveClassChecked(superName, current, context)
          ?: return null
      }
    }

    /*
       3) Otherwise, if C is an interface and the class Object contains a declaration of a public instance method with
       the same name and descriptor as the resolved method, then it is the method to be invoked.
    */
    if (classRef.isInterface) {
      val objectClass = context.classResolver.resolveClassChecked("java/lang/Object", classRef, context)
        ?: return null
      val match = objectClass.methods.find { it.name == resolvedMethod.name && it.descriptor == resolvedMethod.descriptor && it.isPublic }
      if (match != null) {
        return 3 to match
      }
    }

    /*
     4) Otherwise, if there is exactly one maximally-specific method (§5.4.3.3) in the superinterfaces of C that
     matches the resolved method's name and descriptor and is not abstract, then it is the method to be invoked.
    */
    val interfaceMethods = getMaximallySpecificSuperInterfaceMethods(classRef) ?: return null
    val filtered = interfaceMethods.filter { it.name == resolvedMethod.name && it.descriptor == resolvedMethod.descriptor && !it.isAbstract }

    if (filtered.size == 1) {
      return 4 to filtered.single()
    }

    /*
     Otherwise, if step 4 of the lookup procedure determines there are multiple maximally-specific methods in
     the superinterfaces of C that match the resolved method's name and descriptor and are
     not abstract, invokespecial throws an IncompatibleClassChangeError
    */
    if (filtered.size > 1) {
      var implementation1 = filtered[0].location
      var implementation2 = filtered[1].location
      if (implementation1.toString() > implementation2.toString()) {
        val tmp = implementation1
        implementation1 = implementation2
        implementation2 = tmp
      }
      context.problemRegistrar.registerProblem(MultipleDefaultImplementationsProblem(callerMethod.location, methodReference, instruction, implementation1, implementation2))
      return null
    }

    /*
     Otherwise, if step 4 of the lookup procedure determines there are zero maximally-specific methods in the
     superinterfaces of C that match the resolved method's name and descriptor and are not abstract,
     invokespecial throws an AbstractMethodError.
    */
    context.problemRegistrar.registerProblem(AbstractMethodInvocationProblem(methodReference, resolvedMethod.location, callerMethod.location, instruction))
    return null
  }


}