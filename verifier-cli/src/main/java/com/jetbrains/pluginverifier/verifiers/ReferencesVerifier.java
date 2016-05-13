package com.jetbrains.pluginverifier.verifiers;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.CyclicDependenciesProblem;
import com.jetbrains.pluginverifier.problems.MissingDependencyProblem;
import com.jetbrains.pluginverifier.utils.dependencies.*;
import com.jetbrains.pluginverifier.verifiers.clazz.ClassVerifier;
import com.jetbrains.pluginverifier.verifiers.field.FieldVerifier;
import com.jetbrains.pluginverifier.verifiers.instruction.InstructionVerifier;
import com.jetbrains.pluginverifier.verifiers.method.MethodVerifier;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.util.*;

/**
 * @author Dennis.Ushakov
 */
class ReferencesVerifier implements Verifier {

  @Override
  public void verify(@NotNull VerificationContext ctx) {
    Plugin plugin = ctx.getPlugin();

    PluginDependenciesDescriptor descriptor = getPluginDependencies(ctx);
    if (descriptor == null) {
      return;
    }
    processOptionalMissingDependencies(descriptor, ctx);

    try (
        //close the plugin resolver when it is no longer needed (it will delete possibly extracted files)
        Resolver pluginResolver = Resolver.createPluginResolver(plugin)
    ) {
      //don't close this Resolver because it consists of Ide and Jdk resolvers too (they should not be closed yet)
      Resolver pluginClassLoader = createPluginClassLoader(pluginResolver, ctx, descriptor);

      final Resolver resolverForCheck = getResolverForCheck(plugin, pluginResolver);

      //TODO: improve caches
      for (String className : resolverForCheck.getAllClasses()) {
        ClassNode node = VerifierUtil.findClass(resolverForCheck, className, ctx);

        if (node != null) {
          verifyClass(pluginClassLoader, node, ctx);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Unable to create a plugin class loader " + plugin.getPluginId(), e);
    }
  }

  @Nullable
  private PluginDependenciesDescriptor getPluginDependencies(@NotNull VerificationContext ctx) {
    PluginDependenciesDescriptor descriptor;
    try {
      descriptor = DependenciesCache.getInstance().getDependenciesDescriptor(ctx.getPlugin(), ctx.getIde());
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
      return null;
    }
    return descriptor;
  }


  /**
   * Constructs the plugin class-loader class-path.
   * <p>
   * We use the following sequence of searching the class (according to the way IDEA does): <ol> <li>plugin itself
   * (classes and libs)</li> <li>JDK classes</li> <li>IDE /lib classes</li> <li>plugin dependencies</li> </ol>

   * @param pluginResolver pluginResolver
   * @param context verification context
   * @param descriptor dependencies
   * @return resolver
   */
  @NotNull
  private Resolver createPluginClassLoader(@NotNull Resolver pluginResolver,
                                           @NotNull VerificationContext context,
                                           @NotNull PluginDependenciesDescriptor descriptor) {
    Plugin plugin = context.getPlugin();

    List<Resolver> resolvers = new ArrayList<Resolver>();

    resolvers.add(pluginResolver);
    resolvers.add(context.getJdkResolver());
    resolvers.add(context.getIdeResolver());

    for (Plugin dep : descriptor.getDependencies()) {
      try {
        resolvers.add(Resolver.createPluginResolver(dep));
      } catch (IOException e) {
        throw new RuntimeException("Unable to create dependent plugin resolver " + dep.getPluginId(), e);
      }
    }

    if (context.getExternalClassPath() != null) {
      resolvers.add(context.getExternalClassPath());
    }

    String presentableName = "Common resolver for plugin " + plugin.getPluginId() + " with transitive dependencies; ide " + context.getIde().getVersion() + "; jdk " + context.getJdkResolver();
    return Resolver.createCacheResolver(Resolver.createUnionResolver(presentableName, resolvers));
  }

  @NotNull
  private Resolver getResolverForCheck(@NotNull Plugin plugin, @NotNull Resolver pluginResolver) {
    Set<Resolver> usedResolvers = Sets.newIdentityHashSet();

    Set<String> referencedFromXml = new HashSet<String>(plugin.getAllClassesReferencedFromXml());
    plugin.getOptionalDescriptors().values().forEach(x -> referencedFromXml.addAll(x.getAllClassesReferencedFromXml()));

    for (String aClass : referencedFromXml) {
      Resolver location = pluginResolver.getClassLocation(aClass);
      if (location != null) {
        usedResolvers.add(location);
      }
    }

    if (usedResolvers.isEmpty()) {
      return pluginResolver;
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
      for (@SuppressWarnings("unchecked") Iterator<AbstractInsnNode> i = instructions.iterator(); i.hasNext(); ) {
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
