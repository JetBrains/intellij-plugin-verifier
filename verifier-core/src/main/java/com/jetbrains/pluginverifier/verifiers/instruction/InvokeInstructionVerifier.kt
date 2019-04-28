package com.jetbrains.pluginverifier.verifiers.instruction

import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.problems.*
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescriptor
import com.jetbrains.pluginverifier.verifiers.hierarchy.ClassHierarchyBuilder
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.MethodResolver
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.MethodInsnNode

class InvokeInstructionVerifier : InstructionVerifier {
  override fun verify(method: Method, instructionNode: AbstractInsnNode, context: VerificationContext) {
    if (instructionNode !is MethodInsnNode) return
    val instruction = Instruction.fromOpcode(instructionNode.opcode) ?: throw IllegalArgumentException()

    val methodOwner = instructionNode.owner
    if (methodOwner.startsWith("[")) {
      val arrayType = methodOwner.extractClassNameFromDescriptor()
      if (arrayType != null) {
        context.classResolver.resolveClassChecked(arrayType, method, context)
      }
      return
    }

    val ownerClass = context.classResolver.resolveClassChecked(methodOwner, method, context)
    if (ownerClass != null) {
      val methodReference = MethodReference(methodOwner, instructionNode.name, instructionNode.desc)
      InvokeInstructionVerifierImpl(method, ownerClass, methodReference, context, instruction).verify()
    }
  }

}

private class InvokeInstructionVerifierImpl(
    val callerMethod: Method,
    val ownerClass: ClassFile,
    val methodReference: MethodReference,
    val context: VerificationContext,
    val instruction: Instruction
) {

  fun verify() {
    when (instruction) {
      Instruction.INVOKE_VIRTUAL -> processInvokeVirtual()
      Instruction.INVOKE_SPECIAL -> processInvokeSpecial()
      Instruction.INVOKE_INTERFACE -> processInvokeInterface()
      Instruction.INVOKE_STATIC -> processInvokeStatic()
      else -> throw IllegalArgumentException()
    }
  }

  private fun processInvokeVirtual() {
    val method = resolveMethod() ?: return

    if (method.isStatic) {
      /*
      Otherwise, if the resolved method is a class (static) method, the invokevirtual instruction throws an IncompatibleClassChangeError.
       */
      context.problemRegistrar.registerProblem(InvokeInstanceInstructionOnStaticMethodProblem(method.location, callerMethod.location, instruction))
    }
  }

  private fun processInvokeSpecial() {
    /*
    The run-time constant pool item at that index must be a symbolic reference to a method or an interface method (§5.1),
    which gives the name and descriptor (§4.3.3) of the method as well as a symbolic reference
    to the class or interface in which the method is to be found. The named method is resolved.
     */
    val method = resolveMethod() ?: return

    /*
    Otherwise, if the resolved method is an instance initialization method, and the class in which it is declared
    is not the class symbolically referenced by the instruction, a NoSuchMethodError is thrown.
     */
    if (method.name == "<init>" && method.owner.name != methodReference.hostClass.className) {
      registerMethodNotFoundProblem(ownerClass)
    }

    /*
    Otherwise, if the resolved method is a class (static) method,
    the invokespecial instruction throws an IncompatibleClassChangeError.
     */
    if (method.isStatic) {
      context.problemRegistrar.registerProblem(InvokeInstanceInstructionOnStaticMethodProblem(method.location, callerMethod.location, instruction))
    }

    /*
      If all of the following are true, let C be the direct superclass of the current class:
        1) The resolved method is not an instance initialization method (§2.9).
        2) If the symbolic reference names a class (not an interface), then that class is a superclass of the current class.
        2) The ACC_SUPER flag is set for the class file (§4.1).

      Otherwise, let C be the class or interface named by the symbolic reference.

      NOTE! Here is a strange subtlety in the JVM specification: the second condition above was read by me as
       (symbolic reference names a class => that class is a superclass of the current class),
       so I understood conditions as follows:

       1) A
       2) B => C
       3) D

       with the if clause: (A && (not B || C) && D)

       but actually the author wanted to say: (A && (B && C) && D)...

       So I caught up a nasty bug of incorrectly determining the method to be invoked.
    */
    val classRef: ClassFile = if (method.name != "<init>" && (!ownerClass.isInterface && methodReference.hostClass.className == callerMethod.owner.superName) && callerMethod.owner.isSuperFlag) {
      context.classResolver.resolveClassChecked(callerMethod.owner.superName!!, callerMethod, context) ?: return
    } else {
      context.classResolver.resolveClassChecked(methodReference.hostClass.className, callerMethod, context) ?: return
    }

    /*
      The actual method to be invoked is selected by the following lookup procedure:
      */
    val (stepNumber, resolvedMethod) = MethodResolver().lookupSpecialMethod(classRef, methodReference, instruction, callerMethod, context, method)
        ?: return

    /*
    Otherwise, if step 1, step 2, or step 3 of the lookup procedure selects an abstract method, invokespecial throws an AbstractMethodError.
     */
    if (stepNumber in listOf(1, 2, 3) && resolvedMethod.isAbstract) {
      /*
      We intentionally introduce this check because there are the tricky cases when the Java compiler generates
       faulty bytecode. See PR-707 and a test class mock.plugin.noproblems.bridgeMethod.A
       */
      if (!callerMethod.isSynthetic || !callerMethod.isBridgeMethod) {
        context.problemRegistrar.registerProblem(AbstractMethodInvocationProblem(resolvedMethod.location, callerMethod.location, instruction))
      }
    }
  }

  private fun processInvokeInterface() {
    val method = resolveMethod() ?: return

    /**
     * It's a workaround for the fact that we can't compile an interface with a private method.
     */
    fun isTestPrivateInterfaceMethod(method: Method): Boolean =
        System.getProperty("plugin.verifier.test.mode")?.toBoolean() == true
            && method.name == System.getProperty("plugin.verifier.test.private.interface.method.name")

    /*
    Otherwise, if the resolved method is static or private, the invokeinterface instruction throws an IncompatibleClassChangeError.
     */
    if (method.isPrivate || isTestPrivateInterfaceMethod(method)) {
      context.problemRegistrar.registerProblem(InvokeInterfaceOnPrivateMethodProblem(method.location, callerMethod.location))
    }
    if (method.isStatic) {
      context.problemRegistrar.registerProblem(InvokeInstanceInstructionOnStaticMethodProblem(method.location, callerMethod.location, instruction))
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

  private fun processInvokeStatic() {
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
    val method: Method = resolveMethod() ?: return

    /*
    Otherwise, if the resolved method is an instance method, the invokestatic instruction throws an IncompatibleClassChangeError.
     */
    if (!method.isStatic) {
      context.problemRegistrar.registerProblem(InvokeStaticOnInstanceMethodProblem(method.location, callerMethod.location))
    }
  }

  private fun resolveMethod(): Method? =
      MethodResolver().resolveMethod(ownerClass, methodReference, instruction, callerMethod, context)

  private fun registerMethodNotFoundProblem(ownerClass: ClassFile) {
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

}
