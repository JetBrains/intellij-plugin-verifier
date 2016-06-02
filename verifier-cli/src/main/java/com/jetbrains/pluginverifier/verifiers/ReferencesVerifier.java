package com.jetbrains.pluginverifier.verifiers;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginDependency;
import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.MissingDependencyProblem;
import com.jetbrains.pluginverifier.problems.VerificationError;
import com.jetbrains.pluginverifier.utils.Util;
import com.jetbrains.pluginverifier.utils.dependencies.Dependencies;
import com.jetbrains.pluginverifier.utils.dependencies.MissingReason;
import com.jetbrains.pluginverifier.utils.dependencies.PluginDependenciesNode;
import com.jetbrains.pluginverifier.verifiers.clazz.ClassVerifier;
import com.jetbrains.pluginverifier.verifiers.field.FieldVerifier;
import com.jetbrains.pluginverifier.verifiers.instruction.InstructionVerifier;
import com.jetbrains.pluginverifier.verifiers.method.MethodVerifier;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Dennis.Ushakov
 */
class ReferencesVerifier implements Verifier {

  @Override
  public void verify(@NotNull VerificationContext ctx) {
    Plugin plugin = ctx.getPlugin();

    PluginDependenciesNode depNode = getPluginDependencies(ctx);

    boolean hasMissingMandatory = false;
    for (Map.Entry<PluginDependency, MissingReason> entry : depNode.getMissingDependencies().entrySet()) {
      String missingId = entry.getKey().getId();
      if (!ctx.getVerifierOptions().isIgnoreDependency(missingId)) {
        hasMissingMandatory |= !entry.getKey().isOptional();
        ctx.registerProblem(new MissingDependencyProblem(plugin.getPluginId(), missingId, entry.getValue().getReason()), ProblemLocation.fromPlugin(plugin.getPluginId()));
      }
    }

    if (hasMissingMandatory) {
      List<PluginDependency> missing = depNode.getMissingDependencies().keySet().stream().filter(p -> !p.isOptional()).collect(Collectors.toList());
      System.err.println("The plugin verifier will not verify a plugin " + plugin + " because it has missing mandatory dependencies: " + missing);
      return;
    }

    try (
        //close the plugin resolver when it is no longer needed (it will delete possibly extracted files)
        Resolver pluginResolver = Resolver.createPluginResolver(plugin);
        Resolver pluginDependencies = createPluginDependenciesResolver(depNode)
    ) {
      Resolver resolverForCheck = getResolverForCheck(plugin, pluginResolver);

      //don't close this resolver because it contains IDE and JDK resolvers which are not ready to be closed.
      //They will be closed above in the CheckXXXCommand
      Resolver pluginClassLoader = createPluginClassLoader(pluginResolver, pluginDependencies, ctx);

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

  @NotNull
  private PluginDependenciesNode getPluginDependencies(@NotNull VerificationContext ctx) {
    Dependencies.DependenciesResult result = Dependencies.getInstance().calcDependencies(ctx.getPlugin(), ctx.getIde());
    if (result.getCycle() != null && Util.failOnCyclicDependency()) {
      throw new VerificationError("Plugin dependencies tree has the following cycle: " + Joiner.on(" -> ").join(result.getCycle()));
    }
    return result.getDescriptor();
  }

  @NotNull
  private Resolver createPluginDependenciesResolver(@NotNull PluginDependenciesNode descriptor) {
    if (descriptor.getTransitiveDependencies().isEmpty()) {
      return Resolver.getEmptyResolver();
    }
    List<Resolver> depResolvers = new ArrayList<>();
    for (Plugin dep : descriptor.getTransitiveDependencies()) {
      try {
        depResolvers.add(Resolver.createPluginResolver(dep));
      } catch (Exception e) {
        depResolvers.forEach(Resolver::close);
        throw new RuntimeException("Unable to create dependent plugin resolver " + dep.getPluginId(), e);
      }
    }
    return Resolver.createUnionResolver("Plugin " + descriptor.getPlugin() + " transitive dependencies", depResolvers);
  }

  /**
   * Constructs the plugin class-loader class-path.
   * <p>
   * We use the following sequence of searching the class (according to the way IDEA does): <ol> <li>plugin itself
   * (classes and libs)</li> <li>JDK classes</li> <li>IDE /lib classes</li> <li>plugin dependencies</li> </ol>
   *
   * @param pluginResolver pluginResolver
   * @param dependenciesResolver dependenciesResolver
   * @param context verification context
   * @return resolver
   */
  @NotNull
  private Resolver createPluginClassLoader(@NotNull Resolver pluginResolver,
                                           @NotNull Resolver dependenciesResolver,
                                           @NotNull VerificationContext context) {
    Plugin plugin = context.getPlugin();

    List<Resolver> resolvers = new ArrayList<>();

    resolvers.add(pluginResolver);
    resolvers.add(context.getJdkResolver());
    resolvers.add(context.getIdeResolver());
    resolvers.add(dependenciesResolver);

    if (context.getExternalClassPath() != null) {
      resolvers.add(context.getExternalClassPath());
    }

    String presentableName = "Common resolver for plugin " + plugin.getPluginId() + " with transitive dependencies; ide " + context.getIde().getVersion() + "; jdk " + context.getJdkResolver();
    return Resolver.createCacheResolver(Resolver.createUnionResolver(presentableName, resolvers));
  }

  @NotNull
  private Resolver getResolverForCheck(@NotNull Plugin plugin, @NotNull Resolver pluginResolver) {
    Set<Resolver> usedResolvers = Sets.newIdentityHashSet();

    Set<String> referencedFromXml = new HashSet<>(plugin.getAllClassesReferencedFromXml());
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
