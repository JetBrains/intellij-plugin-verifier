package com.jetbrains.pluginverifier.verifiers.instruction;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.verifiers.VContext;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class LdcInstructionVerifier implements InstructionVerifier {
  public void verify(final ClassNode clazz, final MethodNode method, final AbstractInsnNode instr, final Resolver resolver, final VContext ctx) {
    if (!(instr instanceof LdcInsnNode)) return;

    final Object constant = ((LdcInsnNode) instr).cst;
    if (!(constant instanceof Type)) return;

    final String descriptor = ((Type) constant).getDescriptor();
    final String className = VerifierUtil.extractClassNameFromDescr(descriptor);

    if (className == null || VerifierUtil.classExistsOrExternal(ctx, resolver, className)) return;

    ctx.registerProblem(new ClassNotFoundProblem(className), ProblemLocation.fromMethod(clazz.name, method));

    //TODO: process method handle Type: org.objectweb.asm.Type.METHOD
  }
}
