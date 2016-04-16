package com.jetbrains.pluginverifier.verifiers;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.Verifier;
import com.jetbrains.pluginverifier.problems.CyclicDependenciesProblem;
import com.jetbrains.pluginverifier.problems.MissingDependencyProblem;
import com.jetbrains.pluginverifier.results.ProblemLocation;
import com.jetbrains.pluginverifier.utils.dependencies.*;
import com.jetbrains.pluginverifier.verifiers.clazz.ClassVerifier;
import com.jetbrains.pluginverifier.verifiers.field.FieldVerifier;
import com.jetbrains.pluginverifier.verifiers.instruction.InstructionVerifier;
import com.jetbrains.pluginverifier.verifiers.method.MethodVerifier;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * @author Dennis.Ushakov
 */
class ReferencesVerifier implements Verifier {

  @Override
  public void verify(@NotNull VerificationContext ctx) {
    Plugin plugin = ctx.getPlugin();

    PluginDependenciesDescriptor descriptor;
    try {
      descriptor = DependenciesCache.getInstance().getDependenciesDescriptor(ctx.getPlugin(), ctx.getIde(), ctx.getJdk(), ctx.getExternalClassPath());
    } catch (DependenciesError dependenciesError) {
      if (dependenciesError instanceof CyclicDependencyError) {
        CyclicDependenciesProblem problem = new CyclicDependenciesProblem(((CyclicDependencyError) dependenciesError).getCycle());
        ctx.registerProblem(problem, ProblemLocation.fromPlugin(ctx.getPlugin().toString()));
      } else if (dependenciesError instanceof MissingDependenciesError) {
        MissingDependenciesError error = (MissingDependenciesError) dependenciesError;
        MissingDependencyProblem problem = new MissingDependencyProblem(error.getPlugin(), error.getMissedPlugin(), error.getDescription());
        ctx.registerProblem(problem, ProblemLocation.fromPlugin(ctx.getPlugin().toString()));
      } else {
        throw new AssertionError("Forgotten case");
      }
      //we have a missing dependency so unable to verify a plugin fully
      return;
    }

    Resolver commonResolver = Resolver.createCacheResolver(descriptor.getResolver());

    processOptionalMissingDependencies(descriptor, ctx);

    final Resolver resolverForCheck = getResolverForCheck(plugin);

    //TODO: consider traversing all the checked classes in some order which avoids random accessing inner .jar-files
    for (String className : resolverForCheck.getAllClasses()) {
      ClassNode node = VerifierUtil.findClass(resolverForCheck, className, ctx);

      if (node != null) {
        verifyClass(commonResolver, node, ctx);
      }
    }
  }

  @NotNull
  private Resolver getResolverForCheck(@NotNull Plugin plugin) {
    final Resolver commonResolver = plugin.getPluginResolver();

    Set<Resolver> usedResolvers = Sets.newIdentityHashSet();

    Set<String> referencedFromXml = new HashSet<String>(plugin.getAllClassesReferencedFromXml());
    for (Map.Entry<String, Plugin> entry : plugin.getOptionalDescriptors().entrySet()) {
      referencedFromXml.addAll(entry.getValue().getAllClassesReferencedFromXml());
    }

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

  private void processOptionalMissingDependencies(@NotNull PluginDependenciesDescriptor descriptor, @NotNull VerificationContext ctx) {
    String pluginName = descriptor.getPluginName();
    Map<String, String> missingDependencies = descriptor.getMissingOptionalDependencies().get(pluginName);
    if (missingDependencies != null) {
      for (Map.Entry<String, String> entry : missingDependencies.entrySet()) {
        String missingId = entry.getKey();
        String description = entry.getValue();
        if (!ctx.getVerifierOptions().isIgnoreMissingOptionalDependency(missingId)) {
          ctx.registerProblem(new MissingDependencyProblem(pluginName, missingId, description), ProblemLocation.fromPlugin(pluginName));
        }
      }
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
      for (FieldVerifier verifier : Verifiers.getFieldVerifiers()) {
        verifier.verify(node, field, resolver, ctx);
      }
    }
  }
}
