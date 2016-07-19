package com.jetbrains.pluginverifier.verifiers.instruction;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.api.VContext;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.utils.VerifierUtil;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;

/**
 * @author Sergey Patrikeev
 */
public class MultiANewArrayInstructionVerifier implements InstructionVerifier {
  @Override
  public void verify(ClassNode clazz, MethodNode method, AbstractInsnNode instr, Resolver resolver, VContext ctx) {
    if (!(instr instanceof MultiANewArrayInsnNode)) return;
    MultiANewArrayInsnNode newArrayInstruction = (MultiANewArrayInsnNode) instr;
    String descr = VerifierUtil.extractClassNameFromDescr(newArrayInstruction.desc);
    if (descr == null) return;
    if (!VerifierUtil.classExistsOrExternal(ctx, resolver, descr)) {
      ctx.registerProblem(new ClassNotFoundProblem(descr), ProblemLocation.fromMethod(clazz.name, method));
    }
  }
}
