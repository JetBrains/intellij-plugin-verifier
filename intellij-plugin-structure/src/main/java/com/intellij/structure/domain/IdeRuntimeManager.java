package com.intellij.structure.domain;

import com.intellij.structure.impl.domain.JdkManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * @author Sergey Patrikeev
 */
public abstract class IdeRuntimeManager {

  private static IdeRuntimeManager ourInstance;

  @NotNull
  public static IdeRuntimeManager getInstance() {
    if (ourInstance == null) {
      //change if necessary
      ourInstance = new JdkManager();
    }
    return ourInstance;
  }

  @NotNull
  public abstract IdeRuntime createRuntime(@NotNull File runtimeDir) throws IOException;

}
