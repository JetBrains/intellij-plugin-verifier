package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.pool.ClassPool;

public class VerificationContext {
  private ClassPool myIdeaClasses;
  private ClassPool myPluginClasses;

  public VerificationContext(final ClassPool pluginClasses, final ClassPool ideaClasses) {
    myIdeaClasses = ideaClasses;
    myPluginClasses = pluginClasses;
  }

  public ClassPool getIdeaClasses() {
    return myIdeaClasses;
  }

  public ClassPool getPluginClasses() {
    return myPluginClasses;
  }
}
