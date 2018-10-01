package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.problems.AbstractMethodInvocationProblem
import com.jetbrains.pluginverifier.results.problems.InvokeClassMethodOnInterfaceProblem
import com.jetbrains.pluginverifier.results.problems.InvokeInterfaceMethodOnClassProblem
import com.jetbrains.pluginverifier.results.problems.MultipleDefaultImplementationsProblem
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.verifiers.*
import com.jetbrains.pluginverifier.verifiers.logic.CommonClassNames
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.util.*

/**
 * Utility class that implements methods resolution strategies,
 * as described in the JVM specification (§5.4.3.3, §5.4.3.4).
 */
class MethodResolution(
    private val methodReference: MethodReference,
    private val instruction: Instruction,
    private val callerLocation: MethodLocation,
    private val problemRegistrar: ProblemRegistrar,
    private val clsResolver: ClsResolver
) {

  private val methodName = methodReference.methodName

  private val methodDescriptor = methodReference.methodDescriptor

  fun resolveMethod(ownerNode: ClassNode): MethodResolutionResult =
      when (instruction) {
        Instruction.INVOKE_VIRTUAL -> resolveClassMethod(ownerNode)
        Instruction.INVOKE_INTERFACE -> resolveInterfaceMethod(ownerNode)
        Instruction.INVOKE_STATIC -> resolveClassMethod(ownerNode)
        Instruction.INVOKE_SPECIAL -> if (ownerNode.isInterface()) {
          resolveInterfaceMethod(ownerNode)
        } else {
          resolveClassMethod(ownerNode)
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
  private fun resolveInterfaceMethod(interfaceNode: ClassNode): MethodResolutionResult {
    /*
    1) If C is not an interface, interface method resolution throws an IncompatibleClassChangeError.
     */
    if (!interfaceNode.isInterface()) {
      problemRegistrar.registerProblem(InvokeInterfaceMethodOnClassProblem(methodReference, callerLocation, instruction))
      return MethodResolutionResult.Abort
    }

    /*
    2) Otherwise, if C declares a method with the name and descriptor specified by
    the interface method reference, method lookup succeeds.
    */
    val matching = interfaceNode.getMethods().orEmpty().firstOrNull { it.name == methodName && it.desc == methodDescriptor }
    if (matching != null) {
      return MethodResolutionResult.Found(interfaceNode, matching)
    }

    /*
    3) Otherwise, if the class Object declares a method with the name and descriptor specified by the
    interface method reference, which has its ACC_PUBLIC flag set and does not have its ACC_STATIC flag set,
    method lookup succeeds.
    */
    val objectClass = clsResolver.resolveClassOrProblem(CommonClassNames.JAVA_LANG_OBJECT, interfaceNode, problemRegistrar) { interfaceNode.createClassLocation() }
        ?: return MethodResolutionResult.Abort
    val objectMethod = objectClass.getMethods().orEmpty().firstOrNull { it.name == methodName && it.desc == methodDescriptor && it.isPublic() && !it.isStatic() }
    if (objectMethod != null) {
      return MethodResolutionResult.Found(objectClass, objectMethod)
    }

    /*
    4) Otherwise, if the maximally-specific superinterface methods (§5.4.3.3) of C for the name
    and descriptor specified by the method reference include exactly one method that does not
    have its ACC_ABSTRACT flag set, then this method is chosen and method lookup succeeds.
     */
    val maximallySpecificSuperInterfaceMethods = getMaximallySpecificSuperInterfaceMethods(interfaceNode)
        ?: return MethodResolutionResult.Abort
    val single = maximallySpecificSuperInterfaceMethods.singleOrNull { it.methodNode.name == methodName && it.methodNode.desc == methodDescriptor && !it.methodNode.isAbstract() }
    if (single != null) {
      return single
    }

    /*
    5) Otherwise, if any superinterface of C declares a method with the name and descriptor specified by the method
    reference that has neither its ACC_PRIVATE flag nor its ACC_STATIC flag set, one of these is arbitrarily chosen and method lookup succeeds.
     */
    val matchings = getSuperInterfaceMethods(interfaceNode) { it.name == methodName && it.desc == methodDescriptor && !it.isPrivate() && !it.isStatic() }
        ?: return MethodResolutionResult.Abort
    if (matchings.isNotEmpty()) {
      return matchings.first()
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
   * method of C with the specified name and descriptor that is declared in a subinterface of I.
   */
  private fun getMaximallySpecificSuperInterfaceMethods(start: ClassNode): List<MethodResolutionResult.Found>? {
    val predicate: (MethodNode) -> Boolean = { it.name == methodName && it.desc == methodDescriptor && !it.isPrivate() && !it.isStatic() }
    val allMatching = getSuperInterfaceMethods(start, predicate) ?: return null
    return allMatching.filter { (definingClass) ->
      //Check that [definingClass] is not a parent of any other interface.
      allMatching.none { (otherDefiningClass) ->
        otherDefiningClass.name != definingClass.name && clsResolver.isSubclassOf(otherDefiningClass, definingClass, problemRegistrar)
      }
    }
  }

  /**
   * @return all direct and indirect super-interface methods matching the given predicate
   */
  private fun getSuperInterfaceMethods(start: ClassNode, predicate: (MethodNode) -> Boolean): List<MethodResolutionResult.Found>? {
    //breadth-first-search
    val queue: Queue<ClassNode> = LinkedList<ClassNode>()
    val visited = hashSetOf<String>()
    val result = arrayListOf<MethodResolutionResult.Found>()
    queue.add(start)
    visited.add(start.name)
    while (!queue.isEmpty()) {
      val cur = queue.remove()
      cur.getMethods().orEmpty()
          .filter(predicate)
          .mapTo(result) { MethodResolutionResult.Found(cur, it) }

      cur.getInterfaces().orEmpty().forEach {
        if (it !in visited) {
          val resolveClass = clsResolver.resolveClassOrProblem(it, cur, problemRegistrar) { cur.createClassLocation() }
              ?: return null
          visited.add(it)
          queue.add(resolveClass)
        }
      }

      val superName = cur.superName
      if (superName != null) {
        if (superName !in visited) {
          val resolvedSuper = clsResolver.resolveClassOrProblem(superName, cur, problemRegistrar) { cur.createClassLocation() }
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
  private fun resolveClassMethod(classNode: ClassNode): MethodResolutionResult {
    /*
      1) If C is an interface, method resolution throws an IncompatibleClassChangeError.
    */
    if (classNode.isInterface()) {
      /*
      This additional if-condition ensures that we are not trying to resolve a static interface method at the moment.
      It is necessary because actually the JVM 8 spec chooses resolve-class-resolution algorithm unconditionally for resolving
      the static methods of both classes and interface.
      */
      if (instruction != Instruction.INVOKE_STATIC) {
        problemRegistrar.registerProblem(InvokeClassMethodOnInterfaceProblem(methodReference, callerLocation, instruction))
        return MethodResolutionResult.Abort
      }
    }

    /*
      2) Otherwise, method resolution attempts to locate the referenced method in C and its superclasses:
    */
    val lookupResult2 = resolveClassMethodStep2(classNode)
    when (lookupResult2) {
      MethodResolutionResult.Abort -> return lookupResult2
      is MethodResolutionResult.Found -> return lookupResult2
      MethodResolutionResult.NotFound -> Unit
    }

    /*
      3) Otherwise, method resolution attempts to locate the referenced method in the superinterfaces of the specified class C:
    */
    val lookupResult3 = resolveClassMethodStep3(classNode)
    when (lookupResult3) {
      MethodResolutionResult.Abort -> return lookupResult3
      is MethodResolutionResult.Found -> return lookupResult3
      MethodResolutionResult.NotFound -> Unit
    }

    return MethodResolutionResult.NotFound
  }

  private fun resolveClassMethodStep2(currentClass: ClassNode): MethodResolutionResult {
    /*
    2.1) If C declares exactly one method with the name specified by the method reference,
      and the declaration is a signature polymorphic method (§2.9), then method lookup succeeds.

      All the class names mentioned in the descriptor are resolved (§5.4.3.1).

      The resolved method is the signature polymorphic method declaration. It is not necessary for C to declare
      a method with the descriptor specified by the method reference.
    */
    val methods = currentClass.getMethods().orEmpty()

    val matchByName = methods.firstOrNull { it.name == methodName }
    if (matchByName != null && isSignaturePolymorphic(currentClass.name, matchByName) && methods.count { it.name == methodName } == 1) {
      return MethodResolutionResult.Found(currentClass, matchByName)
    }

    /*
    2.2) Otherwise, if C declares a method with the name and descriptor
    specified by the method reference, method lookup succeeds.
     */
    val matching = methods.find { methodName == it.name && methodDescriptor == it.desc }
    if (matching != null) {
      return MethodResolutionResult.Found(currentClass, matching)
    }

    /*
    2.3) Otherwise, if C has a superclass, step 2 of method resolution is recursively invoked
    on the direct superclass of C.
     */
    val superName = currentClass.superName
    if (superName != null) {
      val resolvedSuper = clsResolver.resolveClassOrProblem(superName, currentClass, problemRegistrar) { currentClass.createClassLocation() }
          ?: return MethodResolutionResult.Abort
      val lookupResult = resolveClassMethodStep2(resolvedSuper)
      when (lookupResult) {
        is MethodResolutionResult.Found -> return lookupResult
        MethodResolutionResult.Abort -> return MethodResolutionResult.Abort
        MethodResolutionResult.NotFound -> Unit
      }
    }

    return MethodResolutionResult.NotFound
  }

  private fun resolveClassMethodStep3(currentClass: ClassNode): MethodResolutionResult {

    /*
      3.1) If the maximally-specific superinterface methods of C for the name and descriptor specified
    by the method reference include exactly one method that does not have its ACC_ABSTRACT
    flag set, then this method is chosen and method lookup succeeds.
     */
    val maximallySpecificSuperInterfaceMethods = getMaximallySpecificSuperInterfaceMethods(currentClass)
        ?: return MethodResolutionResult.Abort
    val single = maximallySpecificSuperInterfaceMethods.singleOrNull { it.methodNode.name == methodName && it.methodNode.desc == methodDescriptor && !it.methodNode.isAbstract() }
    if (single != null) {
      return single
    }

    /*
      3.2) Otherwise, if any superinterface of C declares a method with the name and descriptor specified
    by the method reference that has neither its ACC_PRIVATE flag nor its ACC_STATIC
    flag set, one of these is arbitrarily chosen and method lookup succeeds.
     */
    val matchings = getSuperInterfaceMethods(currentClass) { it.name == methodName && it.desc == methodDescriptor && !it.isPrivate() && !it.isStatic() }
        ?: return MethodResolutionResult.Abort
    if (matchings.isNotEmpty()) {
      return matchings.first()
    }

    /*
    3.3) Otherwise, method lookup fails.
     */
    return MethodResolutionResult.NotFound
  }

  /**
   * Resolves actual method to be invoked on 'invokespecial' instruction
   * and the number of successful lookup step.
   */
  fun lookupSpecialMethod(
      classRef: ClassNode,
      resolvedReference: MethodResolutionResult.Found
  ): Pair<Int, MethodResolutionResult.Found>? {
    val resolvedMethod = resolvedReference.methodNode
    /*
      1) If C contains a declaration for an instance method with the same name and descriptor as the resolved method,
      then it is the method to be invoked .
    */
    val matching = classRef.getMethods().orEmpty().find { it.name == resolvedMethod.name && it.desc == resolvedMethod.desc }
    if (matching != null) {
      return 1 to MethodResolutionResult.Found(classRef, matching)
    }

    /*
      2) Otherwise, if C is a class and has a superclass, a search for a declaration of an instance method with the same name
        and descriptor as the resolved method is performed, starting with the direct superclass of C and continuing with the
        direct superclass of that class, and so forth, until a match is found or no further superclasses exist.
        If a match is found, then it is the method to be invoked.
    */
    if (!classRef.isInterface() && classRef.superName != null) {
      var current: ClassNode = clsResolver.resolveClassOrProblem(classRef.superName, classRef, problemRegistrar) { classRef.createClassLocation() }
          ?: return null
      while (true) {
        val match = current.getMethods().orEmpty().find { it.name == resolvedMethod.name && it.desc == resolvedMethod.desc }
        if (match != null) {
          return 2 to MethodResolutionResult.Found(current, match)
        }

        val superName = current.superName
        superName ?: break
        current = clsResolver.resolveClassOrProblem(superName, current, problemRegistrar) { current.createClassLocation() } ?: return null
      }
    }

    /*
       3) Otherwise, if C is an interface and the class Object contains a declaration of a public instance method with
       the same name and descriptor as the resolved method, then it is the method to be invoked.
    */
    if (classRef.isInterface()) {
      val objectClass = clsResolver.resolveClassOrProblem(CommonClassNames.JAVA_LANG_OBJECT, classRef, problemRegistrar) { classRef.createClassLocation() }
          ?: return null
      val match = objectClass.getMethods().orEmpty().find { it.name == resolvedMethod.name && it.desc == resolvedMethod.desc && it.isPublic() }
      if (match != null) {
        return 3 to MethodResolutionResult.Found(objectClass, match)
      }
    }

    /*
     4) Otherwise, if there is exactly one maximally-specific method (§5.4.3.3) in the superinterfaces of C that
     matches the resolved method's name and descriptor and is not abstract, then it is the method to be invoked.
    */
    val interfaceMethods = getMaximallySpecificSuperInterfaceMethods(classRef) ?: return null
    val filtered = interfaceMethods.filter { it.methodNode.name == resolvedMethod.name && it.methodNode.desc == resolvedMethod.desc && !it.methodNode.isAbstract() }

    if (filtered.size == 1) {
      return 4 to filtered.single()
    }

    /*
     Otherwise, if step 4 of the lookup procedure determines there are multiple maximally-specific methods in
     the superinterfaces of C that match the resolved method's name and descriptor and are
     not abstract, invokespecial throws an IncompatibleClassChangeError
    */
    if (filtered.size > 1) {
      var implementation1 = createMethodLocation(filtered[0].definingClass, filtered[0].methodNode)
      var implementation2 = createMethodLocation(filtered[1].definingClass, filtered[1].methodNode)
      if (implementation1.toString() > implementation2.toString()) {
        val tmp = implementation1
        implementation1 = implementation2
        implementation2 = tmp
      }
      problemRegistrar.registerProblem(MultipleDefaultImplementationsProblem(callerLocation, methodReference, instruction, implementation1, implementation2))
      return null
    }

    /*
     Otherwise, if step 4 of the lookup procedure determines there are zero maximally-specific methods in the
     superinterfaces of C that match the resolved method's name and descriptor and are not abstract,
     invokespecial throws an AbstractMethodError.
    */
    val methodRef = createMethodLocation(resolvedReference.definingClass, resolvedReference.methodNode)
    problemRegistrar.registerProblem(AbstractMethodInvocationProblem(methodRef, callerLocation, instruction))
    return null
  }


}