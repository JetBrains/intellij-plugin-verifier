package com.jetbrains.pluginverifier.verifiers.instruction

import com.jetbrains.pluginverifier.results.access.AccessType
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedMethodUsage
import com.jetbrains.pluginverifier.results.experimental.ExperimentalMethodUsage
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.problems.*
import com.jetbrains.pluginverifier.results.reference.SymbolicReference
import com.jetbrains.pluginverifier.verifiers.*
import com.jetbrains.pluginverifier.verifiers.logic.hierarchy.ClassHierarchyBuilder
import com.jetbrains.pluginverifier.verifiers.resolution.MethodResolution
import com.jetbrains.pluginverifier.verifiers.resolution.MethodResolutionResult
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

class InvokeInstructionVerifier : InstructionVerifier {
  override fun verify(clazz: ClassNode, method: MethodNode, instr: AbstractInsnNode, ctx: VerificationContext) {
    if (instr is MethodInsnNode) {
      InvokeImplementation(clazz, method, instr, ctx).verify()
    }
  }

}

@Suppress("UNCHECKED_CAST")
private class InvokeImplementation(
    val verifiableClass: ClassNode,
    val verifiableMethod: MethodNode,
    val instr: MethodInsnNode,
    val ctx: VerificationContext,
    val methodOwner: String = instr.owner,
    val methodName: String = instr.name,
    val methodDescriptor: String = instr.desc
) {
  private val instruction = Instruction.fromOpcode(instr.opcode) ?: throw IllegalArgumentException()

  private val fromMethod = createMethodLocation(verifiableClass, verifiableMethod)

  private val methodResolution = MethodResolution(
      SymbolicReference.methodOf(methodOwner, methodName, methodDescriptor),
      instruction,
      fromMethod,
      ctx,
      ctx.clsResolver
  )

  fun verify() {
    if (methodOwner.startsWith("[")) {
      val arrayType = methodOwner.extractClassNameFromDescr()
      if (arrayType != null) {
        ctx.checkClassExistsOrExternal(arrayType) { fromMethod }
      }
      return
    }
    val ownerNode = ctx.resolveClassOrProblem(methodOwner, verifiableClass) { fromMethod } ?: return

    when (instruction) {
      Instruction.INVOKE_VIRTUAL -> processInvokeVirtual(ownerNode)
      Instruction.INVOKE_SPECIAL -> processInvokeSpecial(ownerNode)
      Instruction.INVOKE_INTERFACE -> processInvokeInterface(ownerNode)
      Instruction.INVOKE_STATIC -> processInvokeStatic(ownerNode)
      else -> throw IllegalArgumentException()
    }
  }

  private fun processInvokeVirtual(ownerNode: ClassNode) {
    val resolved = resolveClassMethod(ownerNode) ?: return

    if (resolved.methodNode.isStatic()) {
      /*
      Otherwise, if the resolved method is a class (static) method, the invokevirtual instruction throws an IncompatibleClassChangeError.
       */
      val methodDeclaration = createMethodLocation(resolved.definingClass, resolved.methodNode)
      val caller = fromMethod
      ctx.registerProblem(InvokeInstanceInstructionOnStaticMethodProblem(methodDeclaration, caller, instruction))
    }
  }

  private fun processInvokeSpecial(ownerNode: ClassNode) {
    /*
    The run-time constant pool item at that index must be a symbolic reference to a method or an interface method (§5.1),
    which gives the name and descriptor (§4.3.3) of the method as well as a symbolic reference
    to the class or interface in which the method is to be found. The named method is resolved.
     */
    val resolved = if (ownerNode.isInterface()) {
      resolveInterfaceMethod(ownerNode)
    } else {
      resolveClassMethod(ownerNode)
    } ?: return

    /*
    Otherwise, if the resolved method is an instance initialization method, and the class in which it is declared
    is not the class symbolically referenced by the instruction, a NoSuchMethodError is thrown.
     */
    if (resolved.methodNode.name == "<init>" && resolved.definingClass.name != methodOwner) {
      registerMethodNotFoundProblem(ownerNode)
    }

    /*
    Otherwise, if the resolved method is a class (static) method,
    the invokespecial instruction throws an IncompatibleClassChangeError.
     */
    if (resolved.methodNode.isStatic()) {
      val resolvedMethod = createMethodLocation(resolved.definingClass, resolved.methodNode)
      ctx.registerProblem(InvokeInstanceInstructionOnStaticMethodProblem(resolvedMethod, fromMethod, instruction))
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
    val classRef: ClassNode = if (resolved.methodNode.name != "<init>" && (!ownerNode.isInterface() && methodOwner == verifiableClass.superName) && verifiableClass.isSuperFlag()) {
      ctx.resolveClassOrProblem(verifiableClass.superName, verifiableClass) { fromMethod } ?: return
    } else {
      ctx.resolveClassOrProblem(methodOwner, verifiableClass) { fromMethod } ?: return
    }

    /*
      The actual method to be invoked is selected by the following lookup procedure:
      */
    val (stepNumber, resolvedMethod) = methodResolution.lookupSpecialMethod(classRef, resolved) ?: return

    /*
    Otherwise, if step 1, step 2, or step 3 of the lookup procedure selects an abstract method, invokespecial throws an AbstractMethodError.
     */
    if (stepNumber in listOf(1, 2, 3) && resolvedMethod.methodNode.isAbstract()) {
      /*
      We intentionally introduce this check because there are the tricky cases when the Java compiler generates
       faulty bytecode. See PR-707 and a test class mock.plugin.noproblems.bridgeMethod.A
       */
      if (!verifiableMethod.isSynthetic() || !verifiableMethod.isBridgeMethod()) {
        val methodDeclaration = createMethodLocation(resolvedMethod.definingClass, resolvedMethod.methodNode)
        ctx.registerProblem(AbstractMethodInvocationProblem(methodDeclaration, fromMethod, instruction))
      }
    }
  }

  private fun processInvokeInterface(ownerNode: ClassNode) {
    val resolved = resolveInterfaceMethod(ownerNode) ?: return

    /**
     * It's a workaround for the fact that we can't compile an interface with a private method.
     */
    fun isTestPrivateInterfaceMethod(method: MethodNode): Boolean =
        System.getProperty("plugin.verifier.test.mode")?.toBoolean() == true
            && method.name == System.getProperty("plugin.verifier.test.private.interface.method.name")

    /*
    Otherwise, if the resolved method is static or private, the invokeinterface instruction throws an IncompatibleClassChangeError.
     */
    if (resolved.methodNode.isPrivate() || isTestPrivateInterfaceMethod(resolved.methodNode)) {
      val resolvedMethod = createMethodLocation(resolved.definingClass, resolved.methodNode)
      ctx.registerProblem(InvokeInterfaceOnPrivateMethodProblem(resolvedMethod, fromMethod))
    }
    if (resolved.methodNode.isStatic()) {
      val resolvedMethod = createMethodLocation(resolved.definingClass, resolved.methodNode)
      ctx.registerProblem(InvokeInstanceInstructionOnStaticMethodProblem(resolvedMethod, fromMethod, instruction))
    }

    /**
     * There are the following additional lookup steps performed during the execution of invokevirtual.
     * The problem is that we don't know the actual type of the objectref at static time, so
     * we should bypass it using the other checks.
     */

    /**
     * Let C be the class of objectref. The actual method to be invoked is selected by the following lookup procedure:
     *
     * 1) If C contains a declaration for an instance method with the same name and descriptor as the resolved method, then it is the method to be invoked.
     *
     * 2) Otherwise, if C has a superclass, a search for a declaration of an instance method with
     * the same name and descriptor as the resolved method is performed, starting with the direct
     * superclass of C and continuing with the direct superclass of that class,
     * and so forth, until a match is found or no further superclasses exist.
     * If a match is found, then it is the method to be invoked.
     *
     * 3) Otherwise, if there is exactly one maximally-specific method (§5.4.3.3) in the superinterfaces of C that
     * matches the resolved method's name and descriptor and is not abstract, then it is the method to be invoked.
     */

    /**
     * And the corresponding Run-Time checks (we can't check them here because we don't have the objectref's class):
     *
     * 1) Otherwise, if the class of objectref does not implement the resolved interface,
     * invokeinterface throws an IncompatibleClassChangeError.
     *
     * 2) Otherwise, if step 1 or step 2 of the lookup procedure selects a method that is
     * not public, invokeinterface throws an IllegalAccessError.
     *
     * 3) Otherwise, if step 1 or step 2 of the lookup procedure selects an abstract
     * method, invokeinterface throws an AbstractMethodError.
     *
     * 4) Otherwise, if step 3 of the lookup procedure determines there are multiple
     * maximally-specific methods in the superinterfaces of C that match the resolved
     * method's name and descriptor and are not abstract, invokeinterface throws an IncompatibleClassChangeError.
     *
     * 5) Otherwise, if step 3 of the lookup procedure determines there are zero maximally-specific
     * methods in the superinterfaces of C that match the resolved method's name and
     * descriptor and are not abstract, invokeinterface throws an AbstractMethodError.
     */
  }

  private fun processInvokeStatic(ownerNode: ClassNode) {
    /**
     * That is a weirdness of the JVM 8 specification. Despite knowing the real class pool
     * item of the invoke instruction (it's either CONSTANT_Methodref_info or CONSTANT_InterfaceMethodref_info)
     * JVM chooses the way to resolve the corresponding method unconditionally (it does always resolves it as a method
     * of the class, not interface).
     * So we must be careful in the resolution steps, because as for JVM 8 Spec the first rule states
     * that "If C is an interface, method resolution throws an IncompatibleClassChangeError." but actually the C could be
     * an interface (Java 8 allows static method in interfaces).
     *
     * This is a corresponding question on stack-overflow:
     * http://stackoverflow.com/questions/42294217/binary-compatibility-of-changing-a-class-with-static-methods-to-interface-in-jav
     */
    val resolved: MethodResolutionResult.Found = resolveClassMethod(ownerNode) ?: return

    /*
    Otherwise, if the resolved method is an instance method, the invokestatic instruction throws an IncompatibleClassChangeError.
     */
    if (!resolved.methodNode.isStatic()) {
      val methodDeclaration = createMethodLocation(resolved.definingClass, resolved.methodNode)
      val caller = fromMethod
      ctx.registerProblem(InvokeStaticOnInstanceMethodProblem(methodDeclaration, caller))
    }
  }

  fun resolveInterfaceMethod(ownerNode: ClassNode): MethodResolutionResult.Found? {
    val lookupResult = methodResolution.resolveMethod(ownerNode)
    return when (lookupResult) {
      is MethodResolutionResult.Found -> {
        /*
         * Otherwise, if method lookup succeeds and the referenced method is not accessible (§5.4.4) to D,
         * method resolution throws an IllegalAccessError.
         */
        checkMethodIsAccessibleOrDeprecated(lookupResult)
      }
      MethodResolutionResult.Abort -> null
      MethodResolutionResult.NotFound -> {
        registerMethodNotFoundProblem(ownerNode)
        null
      }
    }
  }

  fun resolveClassMethod(ownerNode: ClassNode): MethodResolutionResult.Found? {
    val lookupResult = methodResolution.resolveMethod(ownerNode)
    return when (lookupResult) {
      MethodResolutionResult.Abort -> null
      MethodResolutionResult.NotFound -> {
        registerMethodNotFoundProblem(ownerNode)
        null
      }
      is MethodResolutionResult.Found -> {
        /*
       * Otherwise, if method lookup succeeds and the referenced method is not accessible (§5.4.4) to D,
       * method resolution throws an IllegalAccessError.
       */
        checkMethodIsAccessibleOrDeprecated(lookupResult)
      }
    }
  }

  private fun registerMethodNotFoundProblem(ownerNode: ClassNode) {
    val methodReference = SymbolicReference.methodOf(methodOwner, methodName, methodDescriptor)
    val methodOwnerHierarchy = ClassHierarchyBuilder(ctx).buildClassHierarchy(ownerNode)
    ctx.registerProblem(MethodNotFoundProblem(
        methodReference,
        fromMethod,
        instruction,
        methodOwnerHierarchy
    ))
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
  fun checkMethodIsAccessibleOrDeprecated(resolvedMethod: MethodResolutionResult.Found): MethodResolutionResult.Found? {
    val definingClass = resolvedMethod.definingClass
    val methodNode = resolvedMethod.methodNode

    var accessProblem: AccessType? = null

    if (methodNode.isPrivate()) {
      if (verifiableClass.name != definingClass.name) {
        //accessing to private method of the other class
        accessProblem = AccessType.PRIVATE
      }
    } else if (methodNode.isProtected()) {
      if (!haveTheSamePackage(verifiableClass, definingClass)) {
        if (!ctx.isSubclassOf(verifiableClass, definingClass)) {
          accessProblem = AccessType.PROTECTED
        }
      }

    } else if (methodNode.isDefaultAccess()) {
      if (!haveTheSamePackage(definingClass, verifiableClass)) {
        //accessing to the method which is not available in the other package
        accessProblem = AccessType.PACKAGE_PRIVATE
      }
    }

    if (accessProblem != null) {
      val methodDeclaration = createMethodLocation(resolvedMethod.definingClass, resolvedMethod.methodNode)
      val methodReference = SymbolicReference.methodOf(methodOwner, methodName, methodDescriptor)
      val problem = IllegalMethodAccessProblem(methodReference, methodDeclaration, accessProblem, fromMethod, instruction)
      ctx.registerProblem(problem)
      return null
    }
    checkMethodIsUnstable(resolvedMethod)
    return resolvedMethod
  }

  private fun checkMethodIsUnstable(resolvedMethod: MethodResolutionResult.Found) {
    with(resolvedMethod) {
      val methodDeprecated = methodNode.getDeprecationInfo()
      if (methodDeprecated != null) {
        ctx.registerDeprecatedUsage(DeprecatedMethodUsage(createMethodLocation(definingClass, methodNode), fromMethod, methodDeprecated))
      }
      if (methodNode.isExperimentalApi()) {
        ctx.registerExperimentalApiUsage(ExperimentalMethodUsage(createMethodLocation(definingClass, methodNode), fromMethod))
      }
    }
  }

}
