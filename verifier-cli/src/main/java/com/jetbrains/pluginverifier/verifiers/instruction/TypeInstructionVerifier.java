package com.jetbrains.pluginverifier.verifiers.instruction;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.AbstractClassInstantiationProblem;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.problems.InterfaceInstantiationProblem;
import com.jetbrains.pluginverifier.verifiers.VerificationContext;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import jdk.internal.org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Processing of NEW, ANEWARRAY, CHECKCAST and INSTANCEOF instructions.
 *
 * @author Dennis.Ushakov
 */
public class TypeInstructionVerifier implements InstructionVerifier {
  public void verify(final ClassNode clazz, final MethodNode method, final AbstractInsnNode instr, final Resolver resolver, final VerificationContext ctx) {
    if (!(instr instanceof TypeInsnNode)) return;

    TypeInsnNode insnNode = (TypeInsnNode) instr;

    String desc = insnNode.desc;
    String className = VerifierUtil.extractClassNameFromDescr(desc);

    if (className == null) {
      return;
    }

    ClassNode aClass = VerifierUtil.findClass(resolver, className, ctx);
    if (aClass == null) {
      if (!ctx.getVerifierOptions().isExternalClass(className)) {
        ctx.registerProblem(new ClassNotFoundProblem(className), ProblemLocation.fromMethod(clazz.name, method));
      }
      return;
    }


    if (insnNode.getOpcode() == Opcodes.NEW) {
      if (VerifierUtil.isInterface(aClass)) {
        ctx.registerProblem(new InterfaceInstantiationProblem(className), ProblemLocation.fromMethod(clazz.name, method));
      } else if (VerifierUtil.isAbstract(aClass)) {
        ctx.registerProblem(new AbstractClassInstantiationProblem(className), ProblemLocation.fromMethod(clazz.name, method));
      }
    }

  }

}
