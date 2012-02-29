package com.jetbrains.pluginverifier.verifiers.instruction;

import com.jetbrains.pluginverifier.ErrorRegister;
import com.jetbrains.pluginverifier.pool.Resolver;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Dennis.Ushakov
 */
public class InvokeInstructionVerifier implements InstructionVerifier {
  public void verify(final ClassNode clazz, final MethodNode method, final AbstractInsnNode instr, final Resolver resolver, final ErrorRegister register) {
    if (!(instr instanceof MethodInsnNode)) return;
    final MethodInsnNode invoke = (MethodInsnNode)instr;
    if (invoke.name.startsWith("access$")) return;
    if (invoke.owner.startsWith("java/dyn/")) return;
    if (!VerifierUtil.methodExists(resolver, invoke.owner, invoke.name)) {
      register.registerError(resolver.getName(), clazz.name + "." + method.name, "invoking a method " + invoke.owner +  "." + invoke.name + " that doesn't exist ");
    }
  }
}
