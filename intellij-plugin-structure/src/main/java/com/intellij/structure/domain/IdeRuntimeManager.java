package com.intellij.structure.domain;

import com.intellij.structure.impl.domain.JdkManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * @author Sergey Patrikeev
 */
public abstract class IdeRuntimeManager {

  @NotNull
  public static IdeRuntimeManager getJdkManager() {
    return new JdkManager();
  }

  @NotNull
  public abstract Jdk createRuntime(@NotNull File runtimeDir) throws IOException;

}
