package com.jetbrains.pluginverifier.verifiers;

import com.jetbrains.pluginverifier.ErrorRegister;
import com.jetbrains.pluginverifier.Verifier;
import com.jetbrains.pluginverifier.domain.IdeaPlugin;
import com.jetbrains.pluginverifier.resolvers.Resolver;

public class DuplicateClassesVerifier implements Verifier {
  private final ErrorRegister myErrorRegister;
  private final String[] myPrefixesToSkipForDuplicateClassesCheck;
  private final IdeaPlugin myPlugin;

  public DuplicateClassesVerifier(final IdeaPlugin plugin, final ErrorRegister errorRegister, final String[] prefixesToSkipForDuplicateClassesCheck) {
    myPlugin = plugin;
    myErrorRegister = errorRegister;
    myPrefixesToSkipForDuplicateClassesCheck = prefixesToSkipForDuplicateClassesCheck;
  }

  @Override
  public void verify() {
    final Resolver resolverOfDependencies = myPlugin.getResolverOfDependecies();

    for (String className : myPlugin.getClassPool().getAllClasses()) {
      if (skip(className)) {
        continue;
      }

      final String moniker = resolverOfDependencies.getClassLocationMoniker(className);
      if (moniker != null) {
        myErrorRegister.registerError(className, "duplicate class in IDEA classes (" + moniker + ")");
      }
    }
  }

  private boolean skip(final String className) {
    for (String prefix : myPrefixesToSkipForDuplicateClassesCheck) {
      if (prefix != null && prefix.length() > 0 && className.startsWith(prefix)) {
        return true;
      }
    }

    return false;
  }
}
