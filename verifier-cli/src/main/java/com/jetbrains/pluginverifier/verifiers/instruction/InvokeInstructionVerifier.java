package com.jetbrains.pluginverifier.verifiers.instruction;

import com.intellij.structure.resolvers.Resolver;
import com.intellij.structure.resolvers.ResolverUtil;
import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.problems.MethodNotFoundProblem;
import com.jetbrains.pluginverifier.problems.ProblemLocation;
import com.jetbrains.pluginverifier.utils.StringUtil;
import org.jetbrains.annotations.NotNull;
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

    MethodInsnNode invoke = (MethodInsnNode) instr;
    if (invoke.name.startsWith("access$")) //?
      return;

    if (invoke.owner.startsWith("java/dyn/")) //?
      return;

    String className = invoke.owner;

    if (className.startsWith("[")) return;


    if (ctx.getOptions().isExternalClass(className)) return;

    ClassNode classNode = resolver.findClass(className);
    if (classNode == null) {
      ctx.registerProblem(new ClassNotFoundProblem(className), ProblemLocation.fromMethod(clazz.name, method.name + method.desc));
    }
    else {
      ResolverUtil.MethodLocation location = ResolverUtil.findMethod(resolver, classNode, invoke.name, invoke.desc);

      if (location == null || isDefaultConstructorNotFound(invoke, className, location)) {
        String calledMethod = invoke.owner + '#' + invoke.name + invoke.desc;

        if (invoke.owner.equals(clazz.name)) {
          // Looks like method was defined in some parent class
          if (StringUtil.isNotEmpty(classNode.superName) && classNode.interfaces.isEmpty()) {
            calledMethod = classNode.superName + '#' + invoke.name + invoke.desc;
          }
        }

        ctx.registerProblem(new MethodNotFoundProblem(calledMethod),
                            new ProblemLocation(clazz.name, method.name + method.desc));
      }

      //TODO: add access modifier check of invoked method
    }
  }

  private boolean isDefaultConstructorNotFound(@NotNull MethodInsnNode invoke,
                                               @NotNull String className,
                                               @NotNull ResolverUtil.MethodLocation location) {
    return invoke.name.equals("<init>") && invoke.desc.equals("()V") && !location.getClassNode().name.equals(className);
  }
}
