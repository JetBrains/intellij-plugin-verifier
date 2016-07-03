package com.jetbrains.pluginverifier.verifiers.instruction;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.*;
import com.jetbrains.pluginverifier.problems.statics.InvokeInterfaceOnStaticMethodProblem;
import com.jetbrains.pluginverifier.problems.statics.InvokeSpecialOnStaticMethodProblem;
import com.jetbrains.pluginverifier.problems.statics.InvokeStaticOnInstanceMethodProblem;
import com.jetbrains.pluginverifier.problems.statics.InvokeVirtualOnStaticMethodProblem;
import com.jetbrains.pluginverifier.utils.ResolverUtil;
import com.jetbrains.pluginverifier.utils.StringUtil;
import com.jetbrains.pluginverifier.utils.VerifierUtil;
import com.jetbrains.pluginverifier.verifiers.VContext;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static com.jetbrains.pluginverifier.utils.LocationUtils.getMethodLocation;

/**
 * TODO:
 * 1) Instance initialization methods may be invoked only within the Java Virtual Machine by the invokespecial instruction (and check access rights)
 * 2) Signature polymorphic methods may not be in the class (It is not necessary for C to declare a method with the descriptor specified by the method reference)
 * 3) Class Method Resolution vs Interface Method Resolution (If C is not an interface, interface method resolution throws an IncompatibleClassChangeError)
 */
public class InvokeInstructionVerifier implements InstructionVerifier {
  public void verify(final ClassNode clazz, final MethodNode method, final AbstractInsnNode instr, final Resolver resolver, final VContext ctx) {
    if (!(instr instanceof MethodInsnNode))
      return;

    MethodInsnNode invokedMethod = (MethodInsnNode) instr;
    if (invokedMethod.name.startsWith("access$")) {
      return;
    }

    if (invokedMethod.owner.startsWith("java/dyn/")) {
      return;
    }

    String ownerClassName = invokedMethod.owner;

    if (ownerClassName.startsWith("[")) {
      //it's an array class => assume that method exists
      return;
    }

    if (ctx.getVerifierOptions().isExternalClass(ownerClassName)) return;

    ClassNode ownerClass = VerifierUtil.findClass(resolver, clazz, ownerClassName, ctx);

    if (ownerClass == null) {
      ctx.registerProblem(new ClassNotFoundProblem(ownerClassName), ProblemLocation.fromMethod(clazz.name, method));
    } else {
      ResolverUtil.MethodLocation actualLocation = ResolverUtil.findMethod(resolver, ownerClass, invokedMethod.name, invokedMethod.desc, ctx);

      if (actualLocation == null || isDefaultConstructorNotFound(invokedMethod, ownerClassName, actualLocation)) {

        String actualOwner = ownerClassName;

        if (ownerClassName.equals(clazz.name)) {

          // Looks like method was defined in some parent class
          if (StringUtil.isNotEmpty(ownerClass.superName) && ownerClass.interfaces.isEmpty()) {
            //the only possible method holder is a direct parent class
            actualOwner = ownerClass.superName;
          }
        }

        if (VerifierUtil.hasUnresolvedParentClass(actualOwner, resolver, ctx)) {
          //actualOwner has some unresolved class => most likely that this class contains(-ed) the sought-for method
          return;
        }


        String calledMethod = getMethodLocation(ownerClassName, invokedMethod.name, invokedMethod.desc);
        ctx.registerProblem(new MethodNotFoundProblem(calledMethod), ProblemLocation.fromMethod(clazz.name, method));

      } else {
        checkAccessModifier(actualLocation, ctx, resolver, clazz, method);

        checkInvocationType(actualLocation, ctx, clazz, method, invokedMethod);

        //TODO: check that invoked method is not abstract
      }

    }
  }

  private void checkInvocationType(@NotNull ResolverUtil.MethodLocation actualLocation,
                                   @NotNull VContext ctx,
                                   @NotNull ClassNode clazz,
                                   @NotNull MethodNode method,
                                   @NotNull MethodInsnNode invokeInsn) {
    MethodNode actualMethod = actualLocation.getMethodNode();
    ProblemLocation location = ProblemLocation.fromMethod(clazz.name, method);
    ClassNode classNode = actualLocation.getClassNode();
    if (invokeInsn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
      if (VerifierUtil.isStatic(actualMethod)) {
        //attempt to invokevirtual on static method => IncompatibleClassChangeError at runtime

        ctx.registerProblem(new InvokeVirtualOnStaticMethodProblem(getMethodLocation(classNode, actualMethod)), location);
      }
    }

    if (invokeInsn.getOpcode() == Opcodes.INVOKESTATIC) {
      if (!VerifierUtil.isStatic(actualMethod)) {
        //attempt to invokestatic on an instance method => IncompatibleClassChangeError at runtime

        ctx.registerProblem(new InvokeStaticOnInstanceMethodProblem(getMethodLocation(classNode, actualMethod)), location);
      }
    }

    if (invokeInsn.getOpcode() == Opcodes.INVOKEINTERFACE) {
      if (VerifierUtil.isStatic(actualMethod)) {
        ctx.registerProblem(new InvokeInterfaceOnStaticMethodProblem(getMethodLocation(classNode, actualMethod)), location);
      }

      if (VerifierUtil.isPrivate(actualMethod)) {
        ctx.registerProblem(new InvokeInterfaceOnPrivateMethodProblem(getMethodLocation(classNode, actualMethod)), location);
      }
    }

    if (invokeInsn.getOpcode() == Opcodes.INVOKESPECIAL) {
      if (VerifierUtil.isStatic(actualMethod)) {
        ctx.registerProblem(new InvokeSpecialOnStaticMethodProblem(getMethodLocation(classNode, actualMethod)), location);
      }
    }


  }

  private void checkAccessModifier(@NotNull ResolverUtil.MethodLocation actualLocation,
                                   @NotNull VContext ctx,
                                   @NotNull Resolver resolver,
                                   @NotNull ClassNode verifiedClass,
                                   @NotNull MethodNode verifiedMethod) {
    MethodNode actualMethod = actualLocation.getMethodNode();
    ClassNode actualOwner = actualLocation.getClassNode();

    AccessType accessProblem = null;

    if (VerifierUtil.isPrivate(actualMethod)) {
      if (!StringUtil.equals(verifiedClass.name, actualOwner.name)) {
        //accessing to private method of the other class
        accessProblem = AccessType.PRIVATE;
      }
    } else if (VerifierUtil.isProtected(actualMethod)) {
      if (!VerifierUtil.isAncestor(verifiedClass, actualOwner, resolver, ctx) && !VerifierUtil.haveTheSamePackage(actualOwner, verifiedClass)) {
        //accessing to the package-private method of the non-inherited class
        accessProblem = AccessType.PROTECTED;
      }
    } else if (VerifierUtil.isDefaultAccess(actualMethod)) {
      if (!VerifierUtil.haveTheSamePackage(actualOwner, verifiedClass)) {
        //accessing to the method which is not available in the other package
        accessProblem = AccessType.PACKAGE_PRIVATE;
      }
    }

    if (accessProblem != null) {
      IllegalMethodAccessProblem problem = new IllegalMethodAccessProblem(getMethodLocation(actualOwner.name, actualMethod), accessProblem);
      ctx.registerProblem(problem, ProblemLocation.fromMethod(verifiedClass.name, verifiedMethod));
    }
  }

  /**
   * @return true if the default constructor is found in the super-class (but not in the direct owner)
   */
  private boolean isDefaultConstructorNotFound(@NotNull MethodInsnNode invoke,
                                               @NotNull String className,
                                               @NotNull ResolverUtil.MethodLocation location) {
    return invoke.name.equals("<init>") && invoke.desc.equals("()V") && !location.getClassNode().name.equals(className);
  }
}
