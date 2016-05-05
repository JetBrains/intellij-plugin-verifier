package com.jetbrains.pluginverifier.verifiers.instruction;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.verifiers.VerificationContext;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Sergey Patrikeev
 */
public class InvokeDynamicVerifier implements InstructionVerifier {
  @Override
  public void verify(ClassNode clazz, MethodNode method, AbstractInsnNode instr, Resolver resolver, VerificationContext ctx) {
    //TODO: implement
  }
}
