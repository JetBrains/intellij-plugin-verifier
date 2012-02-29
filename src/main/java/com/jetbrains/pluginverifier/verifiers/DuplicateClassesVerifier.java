package com.jetbrains.pluginverifier.verifiers;

import com.jetbrains.pluginverifier.ErrorRegister;
import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.Verifier;
import com.jetbrains.pluginverifier.pool.ClassPool;
import com.jetbrains.pluginverifier.pool.Resolver;

public class DuplicateClassesVerifier implements Verifier {
  private VerificationContext myContext;
  private final ErrorRegister myErrorRegister;
  private final String[] myPrefixesToSkipForDuplicateClassesCheck;

  public DuplicateClassesVerifier(final VerificationContext context, final ErrorRegister errorRegister, final String[] prefixesToSkipForDuplicateClassesCheck) {
    myContext = context;
    myErrorRegister = errorRegister;
    myPrefixesToSkipForDuplicateClassesCheck = prefixesToSkipForDuplicateClassesCheck;
  }

  @Override
  public void verify() {
    Resolver ideaResolver = new Resolver(myContext.getIdeaClasses().getMoniker(), myContext.getIdeaClasses());

    for (String className : myContext.getPluginClasses().getAllClasses()) {
      if (skip(className)) {
        continue;
      }

      final ClassPool classPool = ideaResolver.getClassPool(className);
      if (classPool != null) {
        myErrorRegister.registerError(className, "duplicate class in IDEA classes (" + classPool.getMoniker() + ")");
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
