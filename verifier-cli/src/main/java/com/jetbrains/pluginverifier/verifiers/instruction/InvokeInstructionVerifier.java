package com.jetbrains.pluginverifier.verifiers.instruction;

import com.intellij.structure.resolvers.Resolver;
import com.intellij.structure.resolvers.ResolverUtil;
import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.problems.IllegalMethodAccessProblem;
import com.jetbrains.pluginverifier.problems.MethodNotFoundProblem;
import com.jetbrains.pluginverifier.results.ProblemLocation;
import com.jetbrains.pluginverifier.utils.StringUtil;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Dennis.Ushakov
 */
public class InvokeInstructionVerifier implements InstructionVerifier {
  public void verify(final ClassNode clazz, final MethodNode method, final AbstractInsnNode instr, final Resolver resolver, final VerificationContext ctx) {
    if (!(instr instanceof MethodInsnNode))
      return;

    MethodInsnNode invokedMethod = (MethodInsnNode) instr;
    if (invokedMethod.name.startsWith("access$"))
      return;

    if (invokedMethod.owner.startsWith("java/dyn/"))
      return;

    String ownerClassName = invokedMethod.owner;

    if (ownerClassName.startsWith("[")) return;

    if (ctx.getOptions().isExternalClass(ownerClassName)) return;

    ClassNode ownerClass = resolver.findClass(ownerClassName);
    if (ownerClass == null) {
      ctx.registerProblem(new ClassNotFoundProblem(ownerClassName), ProblemLocation.fromMethod(clazz.name, method.name + method.desc));
    } else {
      ResolverUtil.MethodLocation actualLocation = ResolverUtil.findMethod(resolver, ownerClass, invokedMethod.name, invokedMethod.desc);

      if (actualLocation == null || isDefaultConstructorNotFound(invokedMethod, ownerClassName, actualLocation)) {
        String calledMethod = ownerClassName + '#' + invokedMethod.name + invokedMethod.desc;

        if (ownerClassName.equals(clazz.name)) {
          // Looks like method was defined in some parent class
          if (StringUtil.isNotEmpty(ownerClass.superName) && ownerClass.interfaces.isEmpty()) {
            calledMethod = ownerClass.superName + '#' + invokedMethod.name + invokedMethod.desc;
          }
        }

        ctx.registerProblem(new MethodNotFoundProblem(calledMethod), ProblemLocation.fromMethod(clazz.name, method.name + method.desc));
      } else {
        checkAccessModifier(actualLocation, ctx, resolver, clazz, method);
      }

    }
  }

  private void checkAccessModifier(@NotNull ResolverUtil.MethodLocation actualLocation,
                                   @NotNull VerificationContext ctx,
                                   @NotNull Resolver resolver,
                                   @NotNull ClassNode verifiedClass,
                                   @NotNull MethodNode verifiedMethod) {
    MethodNode actualMethod = actualLocation.getMethodNode();
    ClassNode actualOwner = actualLocation.getClassNode();

    IllegalMethodAccessProblem.MethodAccess accessProblem = null;

    if (VerifierUtil.isPrivate(actualMethod)) {
      if (!StringUtil.equals(verifiedClass.name, actualOwner.name)) {
        //accessing to private method of the other class
        accessProblem = IllegalMethodAccessProblem.MethodAccess.PRIVATE;
      }
    } else if (VerifierUtil.isProtected(actualMethod)) {
      if (!isAncestor(actualOwner, verifiedClass, resolver) && !haveTheSamePackage(actualOwner, verifiedClass)) {
        //accessing to the package-private method of the non-inherited class
        accessProblem = IllegalMethodAccessProblem.MethodAccess.PROTECTED;
      }
    } else if (VerifierUtil.isDefaultAccess(actualMethod)) {
      if (!haveTheSamePackage(actualOwner, verifiedClass)) {
        //accessing to the method which is not available in the other package
        accessProblem = IllegalMethodAccessProblem.MethodAccess.PACKAGE_PRIVATE;
      }
    }

    if (accessProblem != null) {
      IllegalMethodAccessProblem problem = new IllegalMethodAccessProblem(actualOwner.name + "#" + actualMethod.name + actualMethod.desc, accessProblem);
      ctx.registerProblem(problem, new ProblemLocation(verifiedClass.name, verifiedMethod.name + verifiedMethod.desc));
    }
  }

  private boolean haveTheSamePackage(@NotNull ClassNode first, @NotNull ClassNode second) {
    return StringUtil.equals(extractPackage(first.name), extractPackage(second.name));
  }

  private boolean isAncestor(@NotNull ClassNode parent, ClassNode child, @NotNull Resolver resolver) {
    while (child != null) {
      if (StringUtil.equals(parent.name, child.name)) {
        return true;
      }
      String superName = child.superName;
      if (superName == null) {
        return false;
      }
      child = resolver.findClass(superName);
    }
    return false;
  }

  @Nullable
  private String extractPackage(@Nullable String className) {
    if (className == null) return null;
    int slash = className.lastIndexOf('/');
    if (slash == -1) return className;
    return className.substring(0, slash);
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
