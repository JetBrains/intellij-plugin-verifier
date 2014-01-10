package com.jetbrains.pluginverifier.verifiers;

import com.jetbrains.pluginverifier.PluginVerifierOptions;
import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.Verifier;
import com.jetbrains.pluginverifier.domain.IdeaPlugin;
import com.jetbrains.pluginverifier.problems.DuplicateClassProblem;
import com.jetbrains.pluginverifier.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;

public class DuplicateClassesVerifier implements Verifier {

  private boolean skip(final String className, PluginVerifierOptions options) {
    for (String prefix : options.getPrefixesToSkipForDuplicateClassesCheck()) {
      if (prefix != null && prefix.length() > 0 && className.startsWith(prefix)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void verify(@NotNull IdeaPlugin plugin, @NotNull VerificationContext ctx) {
    final Resolver resolverOfDependencies = plugin.getResolverOfDependecies();

    for (String className : plugin.getPluginClassPool().getAllClasses()) {
      if (skip(className, ctx.getOptions())) {
        continue;
      }

      final String moniker = resolverOfDependencies.getClassLocationMoniker(className);
      if (moniker != null) {
        ctx.registerProblem(new DuplicateClassProblem(className, moniker));
      }
    }
  }
}
