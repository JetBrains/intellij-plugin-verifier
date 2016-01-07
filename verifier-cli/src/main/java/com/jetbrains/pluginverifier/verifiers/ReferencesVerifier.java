package com.jetbrains.pluginverifier.verifiers;

import com.intellij.structure.domain.Plugin;
import com.intellij.structure.impl.resolvers.CacheResolver;
import com.intellij.structure.pool.ClassPool;
import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.Verifier;
import com.jetbrains.pluginverifier.error.VerificationError;
import com.jetbrains.pluginverifier.misc.DependenciesCache;
import com.jetbrains.pluginverifier.problems.FailedToReadClassProblem;
import com.jetbrains.pluginverifier.results.ProblemLocation;
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
  public void verify(@NotNull Plugin plugin, @NotNull VerificationContext ctx) throws VerificationError {
    final ClassPool pluginPool = plugin.getPluginClassPool();

    Resolver cacheResolver = new CacheResolver(DependenciesCache.getInstance().getResolver(ctx.getIde(), plugin));

    final Collection<String> classes = pluginPool.getAllClasses();
    for (String className : classes) {
      final ClassNode node = pluginPool.findClass(className);

      if (node == null) {
        ctx.registerProblem(new FailedToReadClassProblem(className), ProblemLocation.fromClass(className));
        continue;
      }

      verifyClass(cacheResolver, node, ctx);
    }
  }

  @SuppressWarnings("unchecked")
  private void verifyClass(@NotNull Resolver resolver, @NotNull ClassNode node, @NotNull VerificationContext ctx) {
    for (ClassVerifier verifier : Verifiers.getClassVerifiers()) {
      verifier.verify(node, resolver, ctx);
    }

    List<MethodNode> methods = (List<MethodNode>) node.methods;
    for (MethodNode method : methods) {
      for (MethodVerifier verifier : Verifiers.getMemberVerifiers()) {
        verifier.verify(node, method, resolver, ctx);
      }

      final InsnList instructions = method.instructions;
      for (Iterator<AbstractInsnNode> i = instructions.iterator(); i.hasNext(); ) {
        AbstractInsnNode instruction = i.next();
        for (InstructionVerifier verifier : Verifiers.getInstructionVerifiers()) {
          verifier.verify(node, method, instruction, resolver, ctx);
        }
      }
    }

    List<FieldNode> fields = (List<FieldNode>) node.fields;
    for (FieldNode field : fields) {
      for (FieldNode method : fields) {
        for (FieldVerifier verifier : Verifiers.getFieldVerifiers()) {
          verifier.verify(node, field, resolver, ctx);
        }
      }
    }
  }
}
