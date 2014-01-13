package com.jetbrains.pluginverifier.verifiers;

import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.Verifier;
import com.jetbrains.pluginverifier.domain.IdeaPlugin;
import com.jetbrains.pluginverifier.pool.ClassPool;
import com.jetbrains.pluginverifier.problems.FailedToReadClassProblem;
import com.jetbrains.pluginverifier.verifiers.clazz.ClassVerifier;
import com.jetbrains.pluginverifier.verifiers.field.FieldVerifier;
import com.jetbrains.pluginverifier.verifiers.instruction.InstructionVerifier;
import com.jetbrains.pluginverifier.verifiers.method.MethodVerifier;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author Dennis.Ushakov
 */
public class ReferencesVerifier implements Verifier {

  @Override
  public void verify(@NotNull IdeaPlugin plugin, @NotNull VerificationContext ctx) {
    final ClassPool pluginPool = plugin.getPluginClassPool();

    final Collection<String> classes = pluginPool.getAllClasses();
    for (String className : classes) {
      final ClassNode node = pluginPool.findClass(className);

      if (node == null) {
        ctx.registerProblem(new FailedToReadClassProblem(className));
        continue;
      }

      verifyClass(plugin, node, ctx);
    }
  }

  private void verifyClass(@NotNull IdeaPlugin plugin, @NotNull ClassNode node, @NotNull VerificationContext ctx) {
    for (ClassVerifier verifier : Verifiers.getClassVerifiers()) {
      verifier.verify(node, plugin.getResolver(), ctx);
    }

    for (MethodNode method : (List<MethodNode>)node.methods) {
      for (MethodVerifier verifier : Verifiers.getMemberVerifiers()) {
        verifier.verify(node, method, plugin.getResolver(), ctx);
      }

      final InsnList instructions = method.instructions;
      for (Iterator<AbstractInsnNode> i = instructions.iterator(); i.hasNext(); ) {
        AbstractInsnNode instruction = i.next();
        for (InstructionVerifier verifier : Verifiers.getInstructionVerifiers()) {
          verifier.verify(node, method, instruction, plugin.getResolver(), ctx);
        }
      }
    }

    for (FieldNode method : (List<FieldNode>)node.fields) {
      for (FieldVerifier verifier : Verifiers.getFieldVerifiers()) {
        verifier.verify(node, method, plugin.getResolver(), ctx);
      }
    }
  }
}
