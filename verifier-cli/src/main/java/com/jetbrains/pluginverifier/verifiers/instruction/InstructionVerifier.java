package com.jetbrains.pluginverifier.verifiers.instruction;

import com.jetbrains.pluginverifier.resolvers.Resolver;
import com.jetbrains.pluginverifier.util.Consumer;
import com.jetbrains.pluginverifier.problems.Problem;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Dennis.Ushakov
 */
public interface InstructionVerifier extends Opcodes {
  void verify(ClassNode clazz, MethodNode method, AbstractInsnNode instr, Resolver resolver, Consumer<Problem> register);
}
