package com.jetbrains.pluginverifier.verifiers.instruction

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.*
import com.jetbrains.pluginverifier.reference.MethodReference
import com.jetbrains.pluginverifier.reference.SymbolicReference
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.jetbrains.intellij.plugins.internal.asm.Opcodes
import org.jetbrains.intellij.plugins.internal.asm.tree.AbstractInsnNode
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.jetbrains.intellij.plugins.internal.asm.tree.MethodInsnNode
import org.jetbrains.intellij.plugins.internal.asm.tree.MethodNode
import java.util.*

class InvokeInstructionVerifier : InstructionVerifier {
  override fun verify(clazz: ClassNode, method: MethodNode, instr: AbstractInsnNode, resolver: Resolver, ctx: VContext) {
    if (instr is MethodInsnNode) {
      InvokeImplementation(clazz, method, instr, resolver, ctx).verify()
    }
  }

}

@Suppress("UNCHECKED_CAST")
private class InvokeImplementation(val verifiableClass: ClassNode,
                                   val verifiableMethod: MethodNode,
                                   val instr: MethodInsnNode,
                                   val resolver: Resolver,
                                   val ctx: VContext,
                                   val methodOwner: String = instr.owner,
                                   val methodName: String = instr.name,
                                   val methodDescriptor: String = instr.desc) {
  var ownerNode: ClassNode? = null

  fun verify() {
    if (methodOwner.startsWith("[")) {
      val arrayType = VerifierUtil.extractClassNameFromDescr(methodOwner)
      if (arrayType != null) {
        VerifierUtil.checkClassExistsOrExternal(resolver, arrayType, ctx, { getFromMethod() })
      }
      return
    }
    ownerNode = VerifierUtil.resolveClassOrProblem(resolver, methodOwner, verifiableClass, ctx, { getFromMethod() }) ?: return

    when (instr.opcode) {
      Opcodes.INVOKEVIRTUAL -> processInvokeVirtual()
      Opcodes.INVOKESPECIAL -> processInvokeSpecial()
      Opcodes.INVOKEINTERFACE -> processInvokeInterface()
      Opcodes.INVOKESTATIC -> processInvokeStatic()
      else -> throw RuntimeException("Unknown opcode for MethodInsnNode ${instr.opcode}: $instr")
    }
  }

  private fun processInvokeVirtual() {
    val resolved = resolveClassMethod() ?: return

    if (VerifierUtil.isStatic(resolved.methodNode)) {
      /*
      Otherwise, if the resolved method is a class (static) method, the invokevirtual instruction throws an IncompatibleClassChangeError.
       */
      ctx.registerProblem(InvokeVirtualOnStaticMethodProblem(SymbolicReference.methodFrom(resolved.definingClass.name, resolved.methodNode.name, resolved.methodNode.desc)), getFromMethod())
    }
  }

  private fun processInvokeSpecial() {
    val resolved: ResolvedMethod
    if (instr.itf) {
      resolved = resolveInterfaceMethod() ?: return
    } else {
      resolved = resolveClassMethod() ?: return
    }

    /*
    Otherwise, if the resolved method is an instance initialization method, and the class in which it is declared
    is not the class symbolically referenced by the instruction, a NoSuchMethodError is thrown.
     */
    if (resolved.methodNode.name == "<init>" && resolved.definingClass.name != methodOwner) {
      ctx.registerProblem(MethodNotFoundProblem(methodOwner, methodName, methodDescriptor), getFromMethod())
    }

    /*
    Otherwise, if the resolved method is a class (static) method,
    the invokespecial instruction throws an IncompatibleClassChangeError.
     */
    if (VerifierUtil.isStatic(resolved.methodNode)) {
      ctx.registerProblem(InvokeSpecialOnStaticMethodProblem(MethodReference(methodOwner, methodName, methodDescriptor)), getFromMethod())
    }

    /*
      If all of the following are true, let C be the direct superclass of the current class:
        1) The resolved method is not an instance initialization method (§2.9).
        2) If the symbolic reference names a class (not an interface), then that class is a superclass of the current class.
        2) The ACC_SUPER flag is set for the class file (§4.1).

      Otherwise, let C be the class or interface named by the symbolic reference.

      NOTE! Here is a strange bug in the JVM specification: the second condition above was read by me as
       (symbolic reference names a class => that class is a superclass of the current class),
       so I understood conditions as follows:

       1) A
       2) B => C
       3) D

       with the if: (A && (not B || C) && D)

       but actually the author wanted to say: (A && (B && C) && D)...

       So I caught up a nasty bug of incorrectly determining the method to be invoked.
    */
    val classRef: ClassNode
    if (resolved.methodNode.name != "<init>" && (!instr.itf && methodOwner == verifiableClass.superName) && VerifierUtil.isSuperFlag(verifiableClass)) {
      classRef = VerifierUtil.resolveClassOrProblem(resolver, verifiableClass.superName, verifiableClass, ctx, { getFromMethod() }) ?: return
    } else {
      classRef = VerifierUtil.resolveClassOrProblem(resolver, methodOwner, verifiableClass, ctx, { getFromMethod() }) ?: return
    }

    /*
      The actual method to be invoked is selected by the following lookup procedure:
      */
    val (stepNumber, resolvedMethod) = invokeSpecialLookup(classRef, resolved.methodNode) ?: return

    /*
    Otherwise, if step 1, step 2, or step 3 of the lookup procedure selects an abstract method, invokespecial throws an AbstractMethodError.
     */
    if (stepNumber in listOf(1, 2, 3) && VerifierUtil.isAbstract(resolvedMethod.methodNode)) {
      ctx.registerProblem(MethodNotImplementedProblem(methodOwner, methodName, methodDescriptor), getFromMethod())
    }
  }


  private fun invokeSpecialLookup(classRef: ClassNode, resolvedMethod: MethodNode): Pair<Int, ResolvedMethod>? {
    /*
      1) If C contains a declaration for an instance method with the same name and descriptor as the resolved method,
      then it is the method to be invoked .
    */
    val matching = (classRef.methods as List<MethodNode>).find { it.name == resolvedMethod.name && it.desc == resolvedMethod.desc }
    if (matching != null) {
      return 1 to ResolvedMethod(classRef, matching)
    }

    /*
      2) Otherwise, if C is a class and has a superclass, a search for a declaration of an instance method with the same name
        and descriptor as the resolved method is performed, starting with the direct superclass of C and continuing with the
        direct superclass of that class, and so forth, until a match is found or no further superclasses exist.
        If a match is found, then it is the method to be invoked.
    */
    if (!VerifierUtil.isInterface(classRef) && classRef.superName != null) {
      var current: ClassNode = VerifierUtil.resolveClassOrProblem(resolver, classRef.superName, classRef, ctx, { ProblemLocation.fromClass(classRef.name) }) ?: return null
      while (true) {
        val match = (current.methods as List<MethodNode>).find { it.name == resolvedMethod.name && it.desc == resolvedMethod.desc }
        if (match != null) {
          return 2 to ResolvedMethod(current, match)
        }

        val superName = current.superName
        superName ?: break
        current = VerifierUtil.resolveClassOrProblem(resolver, superName, current, ctx, { ProblemLocation.fromClass(current.name) }) ?: return null
      }
    }

    /*
       3) Otherwise, if C is an interface and the class Object contains a declaration of a public instance method with
       the same name and descriptor as the resolved method, then it is the method to be invoked.
    */
    if (VerifierUtil.isInterface(classRef)) {
      val objectClass = VerifierUtil.resolveClassOrProblem(resolver, "java/lang/Object", classRef, ctx, { ProblemLocation.fromClass(classRef.name) }) ?: return null
      val match = (objectClass.methods as List<MethodNode>).find { it.name == resolvedMethod.name && it.desc == resolvedMethod.desc && VerifierUtil.isPublic(it) }
      if (match != null) {
        return 3 to ResolvedMethod(objectClass, match)
      }
    }

    /*
       4) Otherwise, if there is exactly one maximally-specific method (§5.4.3.3) in the superinterfaces of C that
       matches the resolved method's name and descriptor and is not abstract, then it is the method to be invoked.
     */
    val interfaceMethods = getMaximallySpecificSuperInterfaceMethods(classRef) ?: return null
    val filtered = interfaceMethods.filter { it.methodNode.name == resolvedMethod.name && it.methodNode.desc == resolvedMethod.desc && !VerifierUtil.isAbstract(it.methodNode) }

    if (filtered.size == 1) {
      return 4 to filtered.single()
    }

    /*
    Otherwise, if step 4 of the lookup procedure determines there are multiple maximally-specific methods in
    the superinterfaces of C that match the resolved method's name and descriptor and are
    not abstract, invokespecial throws an IncompatibleClassChangeError
    */
    if (filtered.size > 1) {
      ctx.registerProblem(MultipleMethodImplementationsProblem(methodOwner, methodName, methodDescriptor), getFromMethod())
      return null
    }

    /*
    Otherwise, if step 4 of the lookup procedure determines there are zero maximally-specific methods in the
    superinterfaces of C that match the resolved method's name and descriptor and are not abstract,
    invokespecial throws an AbstractMethodError.
     */

    ctx.registerProblem(MethodNotImplementedProblem(methodOwner, methodName, methodDescriptor), getFromMethod())
    return null
  }

  private fun processInvokeInterface() {
    val resolved = resolveInterfaceMethod() ?: return

    /*
    Otherwise, if the resolved method is static or private, the invokeinterface instruction throws an IncompatibleClassChangeError.
     */
    if (VerifierUtil.isPrivate(resolved.methodNode)) {
      ctx.registerProblem(InvokeInterfaceOnPrivateMethodProblem(MethodReference(methodOwner, methodName, methodDescriptor)), getFromMethod())
    }
    if (VerifierUtil.isStatic(resolved.methodNode)) {
      ctx.registerProblem(InvokeInterfaceOnStaticMethodProblem(MethodReference(methodOwner, methodName, methodDescriptor)), getFromMethod())
    }
  }

  private fun processInvokeStatic() {
    val resolved: ResolvedMethod
    if (instr.itf) {
      resolved = resolveInterfaceMethod() ?: return
    } else {
      resolved = resolveClassMethod() ?: return
    }

    /*
    Otherwise, if the resolved method is an instance method, the invokestatic instruction throws an IncompatibleClassChangeError.
     */
    if (!VerifierUtil.isStatic(resolved.methodNode)) {
      ctx.registerProblem(InvokeStaticOnInstanceMethodProblem(SymbolicReference.methodFrom(resolved.definingClass.name, resolved.methodNode.name, resolved.methodNode.desc)), getFromMethod())
    }
  }

  private fun getFromMethod() = VerifierUtil.fromMethod(verifiableClass.name, verifiableMethod)

  fun resolveInterfaceMethod(): ResolvedMethod? {
    val (fail, resolvedMethod) = resolveInterfaceMethod0(ownerNode!!)
    if (fail) {
      return null
    }

    if (resolvedMethod == null) {
      ctx.registerProblem(MethodNotFoundProblem(methodOwner, methodName, methodDescriptor), getFromMethod())
      return null
    } else {
      /*
       * Otherwise, if method lookup succeeds and the referenced method is not accessible (§5.4.4) to D,
       * method resolution throws an IllegalAccessError.
       */
      checkMethodIsAccessible(resolvedMethod)
    }
    return resolvedMethod
  }

  fun resolveClassMethod(): ResolvedMethod? {
    val (fail, resolvedMethod) = resolveClassMethod0(ownerNode!!)
    if (fail) {
      return null
    }

    if (resolvedMethod == null) {
      ctx.registerProblem(MethodNotFoundProblem(methodOwner, methodName, methodDescriptor), getFromMethod())
      return null
    } else {
      /*
       * Otherwise, if method lookup succeeds and the referenced method is not accessible (§5.4.4) to D,
       * method resolution throws an IllegalAccessError.
       */
      checkMethodIsAccessible(resolvedMethod)
    }
    return resolvedMethod
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
  fun checkMethodIsAccessible(location: ResolvedMethod) {
    val definingClass = location.definingClass
    val methodNode = location.methodNode

    var accessProblem: AccessType? = null

    if (VerifierUtil.isPrivate(methodNode)) {
      if (verifiableClass.name != definingClass.name) {
        //accessing to private method of the other class
        accessProblem = AccessType.PRIVATE
      }
    } else if (VerifierUtil.isProtected(methodNode)) {
      if (!VerifierUtil.haveTheSamePackage(verifiableClass, definingClass)) {
        if (!VerifierUtil.isSubclassOf(verifiableClass, definingClass, resolver, ctx)) {
          accessProblem = AccessType.PROTECTED
        }
      }

    } else if (VerifierUtil.isDefaultAccess(methodNode)) {
      if (!VerifierUtil.haveTheSamePackage(definingClass, verifiableClass)) {
        //accessing to the method which is not available in the other package
        accessProblem = AccessType.PACKAGE_PRIVATE
      }
    }

    if (accessProblem != null) {
      val problem = IllegalMethodAccessProblem(SymbolicReference.methodFrom(definingClass.name, methodNode.name, methodNode.desc), accessProblem)
      ctx.registerProblem(problem, getFromMethod())
    }
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
  fun resolveInterfaceMethod0(interfaceNode: ClassNode): LookupResult {
    /*
    1) If C is not an interface, interface method resolution throws an IncompatibleClassChangeError.
     */
    if (!VerifierUtil.isInterface(interfaceNode)) {
      ctx.registerProblem(IncompatibleInterfaceToClassChangeProblem(SymbolicReference.classFrom(interfaceNode.name)), getFromMethod())
      return FAILED_LOOKUP
    }

    /*
    2) Otherwise, if C declares a method with the name and descriptor specified by
    the interface method reference, method lookup succeeds.
    */
    val matching = (interfaceNode.methods as List<MethodNode>).firstOrNull { it.name == methodName && it.desc == methodDescriptor }
    if (matching != null) {
      return LookupResult(false, ResolvedMethod(interfaceNode, matching))
    }

    /*
    3) Otherwise, if the class Object declares a method with the name and descriptor specified by the
    interface method reference, which has its ACC_PUBLIC flag set and does not have its ACC_STATIC flag set,
    method lookup succeeds.
    */
    val objectClass = VerifierUtil.resolveClassOrProblem(resolver, "java/lang/Object", interfaceNode, ctx, { ProblemLocation.fromClass(interfaceNode.name) }) ?: return FAILED_LOOKUP
    val objectMethod = (objectClass.methods as List<MethodNode>).firstOrNull { it.name == methodName && it.desc == methodDescriptor && VerifierUtil.isPublic(it) && !VerifierUtil.isStatic(it) }
    if (objectMethod != null) {
      return LookupResult(false, ResolvedMethod(objectClass, objectMethod))
    }

    /*
    4) Otherwise, if the maximally-specific superinterface methods (§5.4.3.3) of C for the name
    and descriptor specified by the method reference include exactly one method that does not
    have its ACC_ABSTRACT flag set, then this method is chosen and method lookup succeeds.
     */
    val maximallySpecificSuperInterfaceMethods = getMaximallySpecificSuperInterfaceMethods(interfaceNode) ?: return FAILED_LOOKUP
    val single = maximallySpecificSuperInterfaceMethods.singleOrNull { it.methodNode.name == methodName && it.methodNode.desc == methodDescriptor && !VerifierUtil.isAbstract(it.methodNode) }
    if (single != null) {
      return LookupResult(false, single)
    }

    /*
    5) Otherwise, if any superinterface of C declares a method with the name and descriptor specified by the method
    reference that has neither its ACC_PRIVATE flag nor its ACC_STATIC flag set, one of these is arbitrarily chosen and method lookup succeeds.
     */
    val matchings = getSuperInterfaceMethods(interfaceNode, { it.name == methodName && it.desc == methodDescriptor && !VerifierUtil.isPrivate(it) && !VerifierUtil.isStatic(it) }) ?: return FAILED_LOOKUP
    if (matchings.isNotEmpty()) {
      return LookupResult(false, matchings.first())
    }

    /*
    6) Otherwise, method lookup fails.
     */
    return NOT_FOUND
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
  private fun getMaximallySpecificSuperInterfaceMethods(start: ClassNode): List<ResolvedMethod>? {
    val predicate: (MethodNode) -> Boolean = { it.name == methodName && it.desc == methodDescriptor && !VerifierUtil.isPrivate(it) && !VerifierUtil.isStatic(it) }
    val allMatching = getSuperInterfaceMethods(start, predicate) ?: return null
    return allMatching.filterIndexed { myIndex, myResolvedMethod ->
      var isDeepest = true
      allMatching.forEachIndexed { otherIndex, otherMethod ->
        if (myIndex != otherIndex && VerifierUtil.isSubclassOf(myResolvedMethod.definingClass, otherMethod.definingClass, resolver, ctx)) {
          isDeepest = false
        }
      }
      isDeepest
    }
  }

  /**
   * @return all direct and indirect super-interface methods matching the given predicate
   */
  private fun getSuperInterfaceMethods(start: ClassNode, predicate: (MethodNode) -> Boolean): List<ResolvedMethod>? {
    //breadth-first-search
    val queue: Queue<ClassNode> = LinkedList<ClassNode>()
    val visited = hashSetOf<String>()
    val result = arrayListOf<ResolvedMethod>()
    queue.add(start)
    visited.add(start.name)
    while (!queue.isEmpty()) {
      val cur = queue.remove()
      val matching = (cur.methods as List<MethodNode>).filter(predicate)
      result.addAll(matching.map { ResolvedMethod(cur, it) })

      (cur.interfaces as List<String>).forEach {
        if (it !in visited) {
          val resolveClass = VerifierUtil.resolveClassOrProblem(resolver, it, cur, ctx, { ProblemLocation.fromClass(cur.name) }) ?: return null
          visited.add(it)
          queue.add(resolveClass)
        }
      }

      val superName = cur.superName
      if (superName != null) {
        if (superName !in visited) {
          val resolvedSuper = VerifierUtil.resolveClassOrProblem(resolver, superName, cur, ctx, { ProblemLocation.fromClass(cur.name) }) ?: return null
          visited.add(superName)
          queue.add(resolvedSuper)
        }
      }
    }
    return result
  }

  /**
   * @return true if success, false otherwise
   */
  private fun dfs0(currentClass: ClassNode, visited: MutableSet<String>): Boolean {
    visited.add(currentClass.name)
    (currentClass.interfaces as List<String>).forEach {
      if (it !in visited) {
        val resolveClass = VerifierUtil.resolveClassOrProblem(resolver, it, currentClass, ctx, { ProblemLocation.fromClass(currentClass.name) })
        if (resolveClass == null || !dfs0(resolveClass, visited)) {
          return false
        }
      }
    }
    return true
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
  fun resolveClassMethod0(classNode: ClassNode): LookupResult {
    /*
      1) If C is an interface, method resolution throws an IncompatibleClassChangeError.
    */
    if (VerifierUtil.isInterface(classNode)) {
      ctx.registerProblem(IncompatibleClassToInterfaceChangeProblem(SymbolicReference.classFrom(classNode.name)), getFromMethod())
      return FAILED_LOOKUP
    }

    /*
      2) Otherwise, method resolution attempts to locate the referenced method in C and its superclasses:
    */
    val (shouldStop2, resolvedMethod2) = resolveClassMethodStep2(classNode)
    if (shouldStop2) {
      return FAILED_LOOKUP
    }
    if (resolvedMethod2 != null) {
      return LookupResult(false, resolvedMethod2)
    }

    /*
      3) Otherwise, method resolution attempts to locate the referenced method in the superinterfaces of the specified class C:
    */
    val (shouldStop3, resolvedMethod3) = resolveClassMethodStep3(classNode)
    if (shouldStop3) {
      return FAILED_LOOKUP
    }
    if (resolvedMethod3 != null) {
      return LookupResult(false, resolvedMethod3)
    }

    return NOT_FOUND
  }

  data class LookupResult(val fail: Boolean, val resolvedMethod: ResolvedMethod?)

  companion object {
    val FAILED_LOOKUP = LookupResult(true, null)
    val NOT_FOUND = LookupResult(false, null)
  }

  fun resolveClassMethodStep2(currentClass: ClassNode): LookupResult {
    /*
    2.1) If C declares exactly one method with the name specified by the method reference,
      and the declaration is a signature polymorphic method (§2.9), then method lookup succeeds.

      All the class names mentioned in the descriptor are resolved (§5.4.3.1).

      The resolved method is the signature polymorphic method declaration. It is not necessary for C to declare
      a method with the descriptor specified by the method reference.
    */
    val methods = currentClass.methods as List<MethodNode>

    val matchByName = methods.firstOrNull { it.name == methodName }
    if (matchByName != null && VerifierUtil.isSignaturePolymorphic(currentClass.name, matchByName) && methods.count { it.name == methodName } == 1) {
      return LookupResult(false, ResolvedMethod(currentClass, matchByName))
    }

    /*
    2.2) Otherwise, if C declares a method with the name and descriptor
    specified by the method reference, method lookup succeeds.
     */
    val matching = methods.find { methodName == it.name && methodDescriptor == it.desc }
    if (matching != null) {
      return LookupResult(false, ResolvedMethod(currentClass, matching))
    }

    /*
    2.3) Otherwise, if C has a superclass, step 2 of method resolution is recursively invoked
    on the direct superclass of C.
     */
    val superName = currentClass.superName
    if (superName != null) {
      val resolvedSuper = VerifierUtil.resolveClassOrProblem(resolver, superName, currentClass, ctx, { ProblemLocation.fromClass(currentClass.name) }) ?: return LookupResult(true, null)
      val (shouldStopLookup, resolvedMethod) = resolveClassMethodStep2(resolvedSuper)
      if (shouldStopLookup) {
        return FAILED_LOOKUP
      }
      return LookupResult(false, resolvedMethod)
    }

    return NOT_FOUND
  }

  fun resolveClassMethodStep3(currentClass: ClassNode): LookupResult {

    /*
      3.1) If the maximally-specific superinterface methods of C for the name and descriptor specified
    by the method reference include exactly one method that does not have its ACC_ABSTRACT
    flag set, then this method is chosen and method lookup succeeds.
     */
    val maximallySpecificSuperInterfaceMethods = getMaximallySpecificSuperInterfaceMethods(currentClass) ?: return FAILED_LOOKUP
    val single = maximallySpecificSuperInterfaceMethods.singleOrNull { it.methodNode.name == methodName && it.methodNode.desc == methodDescriptor && !VerifierUtil.isAbstract(it.methodNode) }
    if (single != null) {
      return LookupResult(false, single)
    }

    /*
      3.2) Otherwise, if any superinterface of C declares a method with the name and descriptor specified
    by the method reference that has neither its ACC_PRIVATE flag nor its ACC_STATIC
    flag set, one of these is arbitrarily chosen and method lookup succeeds.
     */
    val matchings = getSuperInterfaceMethods(currentClass, { it.name == methodName && it.desc == methodDescriptor && !VerifierUtil.isPrivate(it) && !VerifierUtil.isStatic(it) }) ?: return FAILED_LOOKUP
    if (matchings.isNotEmpty()) {
      return LookupResult(false, matchings.first())
    }

    /*
    3.3) Otherwise, method lookup fails.
     */
    return NOT_FOUND
  }

  data class ResolvedMethod(val definingClass: ClassNode, val methodNode: MethodNode)

}
