/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.instruction

import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.problems.AbstractMethodInvocationProblem
import com.jetbrains.pluginverifier.results.problems.InvokeInstanceInstructionOnStaticMethodProblem
import com.jetbrains.pluginverifier.results.problems.InvokeInterfaceOnPrivateMethodProblem
import com.jetbrains.pluginverifier.results.problems.InvokeStaticOnInstanceMethodProblem
import com.jetbrains.pluginverifier.results.problems.MethodNotFoundProblem
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.hierarchy.ClassHierarchyBuilder
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.MethodResolver
import com.jetbrains.pluginverifier.verifiers.resolution.resolveClassChecked
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode

class MethodInvokeInstructionVerifier(
  private val callerMethod: Method,
  private val methodOwnerClass: ClassFile,
  private val methodReference: MethodReference,
  private val context: VerificationContext,
  private val instruction: Instruction,
  private val instructionNode: AbstractInsnNode
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
      context.problemRegistrar.registerProblem(InvokeInstanceInstructionOnStaticMethodProblem(methodReference, method.location, callerMethod.location, instruction))
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
    if (method.name == "<init>" && method.containingClassFile.name != methodReference.hostClass.className) {
      registerMethodNotFoundProblem(methodOwnerClass)
    }

    /*
    Otherwise, if the resolved method is a class (static) method,
    the invokespecial instruction throws an IncompatibleClassChangeError.
     */
    if (method.isStatic) {
      context.problemRegistrar.registerProblem(InvokeInstanceInstructionOnStaticMethodProblem(methodReference, method.location, callerMethod.location, instruction))
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
    val classRef: ClassFile = if (method.name != "<init>" && (!methodOwnerClass.isInterface && methodReference.hostClass.className == callerMethod.containingClassFile.superName) && callerMethod.containingClassFile.isSuperFlag) {
      context.classResolver.resolveClassChecked(callerMethod.containingClassFile.superName!!, callerMethod, context)
        ?: return
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
        context.problemRegistrar.registerProblem(AbstractMethodInvocationProblem(methodReference, resolvedMethod.location, callerMethod.location, instruction))
      }
    }
  }

  private fun processInvokeInterface() {
    val method = resolveMethod() ?: return

    /**
     * It's a workaround for the fact that we can't compile an interface with a private method.
     */
    fun isTestPrivateInterfaceMethod(method: Method): Boolean =
      method.name == System.getProperty("plugin.verifier.test.private.interface.method.name")

    /*
    Otherwise, if the resolved method is `static` or `private`, the `INVOKEINTERFACE` instruction
    throws an `IncompatibleClassChangeError`.
    However, Java 17 and other compilers might invoke such methods when using `INVOKEDYNAMIC`.
    Usually, this happens when invoking lambdas that are compiled as `private synthetic` methods.
     */
    if ((method.isPrivate || isTestPrivateInterfaceMethod(method)) && !isViaInvokeDynamic) {
      context.problemRegistrar.registerProblem(
        InvokeInterfaceOnPrivateMethodProblem(
          methodReference,
          method.location,
          callerMethod.location
        )
      )
    }
    if (method.isStatic) {
      context.problemRegistrar.registerProblem(InvokeInstanceInstructionOnStaticMethodProblem(methodReference, method.location, callerMethod.location, instruction))
    }

    /**
     * There are lookup steps performed during the execution of invokevirtual.
     * The problem is that we don't know the actual type of the objectref at static time,
     * so we should bypass it using other checks.
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
     * https://stackoverflow.com/questions/42294217/binary-compatibility-of-changing-a-class-with-static-methods-to-interface-in-jav
     */
    val method: Method = resolveMethod() ?: return

    /*
    Otherwise, if the resolved method is an instance method, the invokestatic instruction throws an IncompatibleClassChangeError.
     */
    if (!method.isStatic) {
      context.problemRegistrar.registerProblem(InvokeStaticOnInstanceMethodProblem(methodReference, method.location, callerMethod.location))
    }
  }

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

  private fun resolveMethod(): Method? {
    val method = MethodResolver().resolveMethod(methodOwnerClass, methodReference, instruction, callerMethod, context)
    if (method != null) {
      context.apiUsageProcessors.forEach { it.processMethodInvocation(methodReference, method, instructionNode, callerMethod, context) }
    }
    return method
  }

  private val isViaInvokeDynamic: Boolean
    get() = instructionNode.opcode == Opcodes.INVOKEDYNAMIC
}