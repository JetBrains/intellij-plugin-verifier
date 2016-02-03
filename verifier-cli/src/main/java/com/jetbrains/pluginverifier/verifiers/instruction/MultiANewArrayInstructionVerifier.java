package com.jetbrains.pluginverifier.verifiers.instruction;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.results.ProblemLocation;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;

/**
 * @author Sergey Patrikeev
 */
public class MultiANewArrayInstructionVerifier implements InstructionVerifier {
  @Override
  public void verify(ClassNode clazz, MethodNode method, AbstractInsnNode instr, Resolver resolver, VerificationContext ctx) {
    if (!(instr instanceof MultiANewArrayInsnNode)) return;
    MultiANewArrayInsnNode newArrayInstruction = (MultiANewArrayInsnNode) instr;
    String descr = VerifierUtil.extractClassNameFromDescr(newArrayInstruction.desc);
    if (descr == null) return;
    if (!VerifierUtil.classExists(ctx.getVerifierOptions(), resolver, descr)) {
      ctx.registerProblem(new ClassNotFoundProblem(descr), ProblemLocation.fromMethod(clazz.name, method));
    }
  }
}
