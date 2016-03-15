package com.jetbrains.pluginverifier.verifiers;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.Verifier;
import com.jetbrains.pluginverifier.error.VerificationError;
import com.jetbrains.pluginverifier.misc.DependenciesCache;
import com.jetbrains.pluginverifier.problems.FailedToReadClassProblem;
import com.jetbrains.pluginverifier.problems.MissingDependencyProblem;
import com.jetbrains.pluginverifier.results.ProblemLocation;
import com.jetbrains.pluginverifier.verifiers.clazz.ClassVerifier;
import com.jetbrains.pluginverifier.verifiers.field.FieldVerifier;
import com.jetbrains.pluginverifier.verifiers.instruction.InstructionVerifier;
import com.jetbrains.pluginverifier.verifiers.method.MethodVerifier;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Dennis.Ushakov
 */
class ReferencesVerifier implements Verifier {

  @Override
  public void verify(@NotNull Plugin plugin, @NotNull VerificationContext ctx) throws VerificationError {

    DependenciesCache.PluginDependenciesDescriptor descriptor = DependenciesCache.getInstance().getResolver(plugin, ctx.getIde(), ctx.getJdk(), ctx.getExternalClassPath());
    Resolver cacheResolver = Resolver.createCacheResolver(descriptor.getResolver());

    processMissingDependencies(descriptor, ctx);

    final Resolver resolverForCheck = getResolverForCheck(plugin);

    for (String className : resolverForCheck.getAllClasses()) {
      ClassNode node = VerifierUtil.findClass(resolverForCheck, className);

      if (node == null) {
        ctx.registerProblem(new FailedToReadClassProblem(className), ProblemLocation.fromClass(className));
        continue;
      }

      verifyClass(cacheResolver, node, ctx);
    }
  }

  @NotNull
  private Resolver getResolverForCheck(@NotNull Plugin plugin) {
    final Resolver commonResolver = plugin.getPluginResolver();

    Set<Resolver> usedResolvers = Sets.newIdentityHashSet();

    Set<String> referencedFromXml = plugin.getAllClassesReferencedFromXml();
    for (String aClass : referencedFromXml) {
      Resolver location = commonResolver.getClassLocation(aClass);
      if (location != null) {
        usedResolvers.add(location);
      }
    }

    if (usedResolvers.isEmpty()) {
      return commonResolver;
    }

    return Resolver.createUnionResolver("Plugin classes for check", Lists.newArrayList(usedResolvers));
  }

  private void processMissingDependencies(@NotNull DependenciesCache.PluginDependenciesDescriptor descriptor, @NotNull VerificationContext ctx) {
    String pluginName = descriptor.getPluginName();
    Map<String, String> missingDependencies = descriptor.getMissingDependencies().get(pluginName);
    if (missingDependencies != null) {
      for (Map.Entry<String, String> entry : missingDependencies.entrySet()) {
        String missingId = entry.getKey();
        if (!ctx.getVerifierOptions().isIgnoreMissingOptionalDependency(missingId)) {
          ctx.registerProblem(new MissingDependencyProblem(missingId, entry.getValue()), ProblemLocation.fromPlugin(pluginName));
        }
      }
    }


  }

  @SuppressWarnings("unchecked")
  private void verifyClass(@NotNull Resolver resolver, @NotNull ClassNode node, @NotNull VerificationContext ctx) throws VerificationError {
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
      for (FieldVerifier verifier : Verifiers.getFieldVerifiers()) {
        verifier.verify(node, field, resolver, ctx);
      }
    }
  }
}
