package com.jetbrains.pluginverifier.verifiers.instruction;

import com.jetbrains.pluginverifier.pool.ResolverUtil;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.problems.MethodNotFoundProblem;
import com.jetbrains.pluginverifier.resolvers.Resolver;
import com.jetbrains.pluginverifier.util.Consumer;
import com.jetbrains.pluginverifier.problems.Problem;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Dennis.Ushakov
 */
public class InvokeInstructionVerifier implements InstructionVerifier {
  public void verify(final ClassNode clazz, final MethodNode method, final AbstractInsnNode instr, final Resolver resolver, final Consumer<Problem> register) {
    if (!(instr instanceof MethodInsnNode))
      return;

    MethodInsnNode invoke = (MethodInsnNode) instr;
    if (invoke.name.startsWith("access$"))
      return;

    if (invoke.owner.startsWith("java/dyn/"))
      return;

    String className = invoke.owner;

    if (className.startsWith("[")) return;

    ClassNode classNode = resolver.findClass(className);
    if (classNode == null) {
      register.consume(new ClassNotFoundProblem(clazz.name, method.name + method.desc, className));
    }
    else {
      if (ResolverUtil.findMethod(resolver, classNode, invoke.name, invoke.desc) == null) {
        register.consume(new MethodNotFoundProblem(clazz.name,
                                                   method.name + method.desc,
                                                   invoke.owner + '#' + invoke.name + invoke.desc));
      }
    }
  }
}
