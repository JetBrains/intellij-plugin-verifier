package com.jetbrains.pluginverifier.verifiers.instruction;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.results.ProblemLocation;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Sergey Patrikeev
 */
public class FieldAccessInstructionVerifier implements InstructionVerifier {
  @Override
  public void verify(ClassNode clazz, MethodNode method, AbstractInsnNode instr, Resolver resolver, VerificationContext ctx) {
    if (!(instr instanceof FieldInsnNode)) return;
    FieldInsnNode node = (FieldInsnNode) instr;

    String fieldName = node.name;
    String fieldOwner = node.owner;

    fieldOwner = VerifierUtil.extractClassNameFromDescr(fieldOwner);
    if (fieldOwner == null) return;
    if (!VerifierUtil.classExists(ctx.getOptions(), resolver, fieldOwner)) {
      ctx.registerProblem(new ClassNotFoundProblem(fieldOwner), ProblemLocation.fromMethod(clazz.name, method));
      return;
    }

    int opcode = node.getOpcode();
    switch (opcode) {
      case Opcodes.GETSTATIC:
      case Opcodes.PUTSTATIC:
        //TODO:
        break;

      case Opcodes.GETFIELD:
      case Opcodes.PUTFIELD:
        //TODO:
        break;
    }

  }
}
