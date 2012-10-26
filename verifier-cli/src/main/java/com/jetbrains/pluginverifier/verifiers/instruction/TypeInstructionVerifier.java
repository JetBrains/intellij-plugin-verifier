package com.jetbrains.pluginverifier.verifiers.instruction;

import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.resolvers.Resolver;
import com.jetbrains.pluginverifier.util.Consumer;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * @author Dennis.Ushakov
 */
public class TypeInstructionVerifier implements InstructionVerifier {
  public void verify(final ClassNode clazz, final MethodNode method, final AbstractInsnNode instr, final Resolver resolver, final Consumer<Problem> register) {
    if (!(instr instanceof TypeInsnNode)) return;
    final String className = ((TypeInsnNode)instr).desc;
    if(className == null || VerifierUtil.classExists(resolver, className)) return;

    register.consume(new ClassNotFoundProblem(clazz.name, method.name + method.desc, className));
  }
}
