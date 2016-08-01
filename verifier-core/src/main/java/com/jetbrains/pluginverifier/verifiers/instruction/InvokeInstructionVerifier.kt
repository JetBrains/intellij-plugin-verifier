package com.jetbrains.pluginverifier.verifiers.instruction

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.*
import com.jetbrains.pluginverifier.problems.statics.InvokeInterfaceOnStaticMethodProblem
import com.jetbrains.pluginverifier.problems.statics.InvokeSpecialOnStaticMethodProblem
import com.jetbrains.pluginverifier.problems.statics.InvokeStaticOnInstanceMethodProblem
import com.jetbrains.pluginverifier.problems.statics.InvokeVirtualOnStaticMethodProblem
import com.jetbrains.pluginverifier.utils.LocationUtils
import com.jetbrains.pluginverifier.utils.ResolverUtil
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

/**
 * TODO:
 * 1) Instance initialization methods may be invoked only within the Java Virtual Machine by the invokespecial instruction (and check access rights)
 * 2) Signature polymorphic methods may not be in the class (It is not necessary for C to declare a method with the descriptor specified by the method reference)
 * 3) Class Method Resolution vs Interface Method Resolution (If C is not an interface, interface method resolution throws an IncompatibleClassChangeError)
 */
class InvokeInstructionVerifier : InstructionVerifier {
  override fun verify(clazz: ClassNode, method: MethodNode, instr: AbstractInsnNode, resolver: Resolver, ctx: VContext) {
    if (instr !is MethodInsnNode)
      return

    if (instr.name.startsWith("access$")) {
      return
    }

    if (instr.owner.startsWith("java/dyn/")) {
      return
    }

    val ownerClassName = instr.owner

    if (ownerClassName.startsWith("[")) {
      //it's an array class => assume that method exists
      return
    }

    if (ctx.verifierOptions.isExternalClass(ownerClassName)) return

    val ownerClass = VerifierUtil.findClass(resolver, clazz, ownerClassName, ctx)

    if (ownerClass == null) {
      ctx.registerProblem(ClassNotFoundProblem(ownerClassName), ProblemLocation.fromMethod(clazz.name, method))
    } else {
      val actualLocation = ResolverUtil.findMethod(resolver, ownerClass, instr.name, instr.desc, ctx)

      if (actualLocation == null || isDefaultConstructorNotFound(instr, ownerClassName, actualLocation)) {

        var actualOwner = ownerClassName

        if (ownerClassName == clazz.name) {

          // Looks like method was defined in some parent class
          if (ownerClass.superName != null && !ownerClass.superName.isEmpty() && ownerClass.interfaces.isEmpty()) {
            //the only possible method holder is a direct parent class
            actualOwner = ownerClass.superName
          }
        }

        if (VerifierUtil.hasUnresolvedParentClass(actualOwner, resolver, ctx)) {
          //actualOwner has some unresolved class => most likely that this class contains(-ed) the sought-for method
          return
        }


        val calledMethod = LocationUtils.getMethodLocation(ownerClassName, instr.name, instr.desc)
        ctx.registerProblem(MethodNotFoundProblem(calledMethod), ProblemLocation.fromMethod(clazz.name, method))

      } else {
        checkAccessModifier(actualLocation, ctx, resolver, clazz, method)

        checkInvocationType(actualLocation, ctx, clazz, method, instr)

        //TODO: check that invoked method is not abstract
      }

    }
  }

  private fun checkInvocationType(actualLocation: ResolverUtil.MethodLocation,
                                  ctx: VContext,
                                  clazz: ClassNode,
                                  method: MethodNode,
                                  invokeInsn: MethodInsnNode) {
    val actualMethod = actualLocation.methodNode
    val location = ProblemLocation.fromMethod(clazz.name, method)
    val classNode = actualLocation.classNode
    if (invokeInsn.opcode == Opcodes.INVOKEVIRTUAL) {
      if (VerifierUtil.isStatic(actualMethod)) {
        //attempt to invokevirtual on static method => IncompatibleClassChangeError at runtime

        ctx.registerProblem(InvokeVirtualOnStaticMethodProblem(LocationUtils.getMethodLocation(classNode, actualMethod)), location)
      }
    }

    if (invokeInsn.opcode == Opcodes.INVOKESTATIC) {
      if (!VerifierUtil.isStatic(actualMethod)) {
        //attempt to invokestatic on an instance method => IncompatibleClassChangeError at runtime

        ctx.registerProblem(InvokeStaticOnInstanceMethodProblem(LocationUtils.getMethodLocation(classNode, actualMethod)), location)
      }
    }

    if (invokeInsn.opcode == Opcodes.INVOKEINTERFACE) {
      if (VerifierUtil.isStatic(actualMethod)) {
        ctx.registerProblem(InvokeInterfaceOnStaticMethodProblem(LocationUtils.getMethodLocation(classNode, actualMethod)), location)
      }

      if (VerifierUtil.isPrivate(actualMethod)) {
        ctx.registerProblem(InvokeInterfaceOnPrivateMethodProblem(LocationUtils.getMethodLocation(classNode, actualMethod)), location)
      }
    }

    if (invokeInsn.opcode == Opcodes.INVOKESPECIAL) {
      if (VerifierUtil.isStatic(actualMethod)) {
        ctx.registerProblem(InvokeSpecialOnStaticMethodProblem(LocationUtils.getMethodLocation(classNode, actualMethod)), location)
      }
    }


  }

  private fun checkAccessModifier(actualLocation: ResolverUtil.MethodLocation,
                                  ctx: VContext,
                                  resolver: Resolver,
                                  verifiedClass: ClassNode,
                                  verifiedMethod: MethodNode) {
    val actualMethod = actualLocation.methodNode
    val actualOwner = actualLocation.classNode

    var accessProblem: AccessType? = null

    if (VerifierUtil.isPrivate(actualMethod)) {
      if (!verifiedClass.name.equals(actualOwner.name, false)) {
        //accessing to private method of the other class
        accessProblem = AccessType.PRIVATE
      }
    } else if (VerifierUtil.isProtected(actualMethod)) {
      if (!VerifierUtil.isAncestor(verifiedClass, actualOwner, resolver, ctx) && !VerifierUtil.haveTheSamePackage(actualOwner, verifiedClass)) {
        //accessing to the package-private method of the non-inherited class
        accessProblem = AccessType.PROTECTED
      }
    } else if (VerifierUtil.isDefaultAccess(actualMethod)) {
      if (!VerifierUtil.haveTheSamePackage(actualOwner, verifiedClass)) {
        //accessing to the method which is not available in the other package
        accessProblem = AccessType.PACKAGE_PRIVATE
      }
    }

    if (accessProblem != null) {
      val problem = IllegalMethodAccessProblem(LocationUtils.getMethodLocation(actualOwner.name, actualMethod), accessProblem)
      ctx.registerProblem(problem, ProblemLocation.fromMethod(verifiedClass.name, verifiedMethod))
    }
  }

  /**
   * @return true if the default constructor is found in the super-class (but not in the direct owner)
   */
  private fun isDefaultConstructorNotFound(invoke: MethodInsnNode,
                                           className: String,
                                           location: ResolverUtil.MethodLocation): Boolean {
    return invoke.name == "<init>" && invoke.desc == "()V" && location.classNode.name != className
  }
}
