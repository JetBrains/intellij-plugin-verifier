package com.intellij.structure.impl.domain;

import com.intellij.structure.domain.IdeRuntime;
import com.intellij.structure.domain.IdeRuntimeManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * @author Sergey Patrikeev
 */
public class JdkManager extends IdeRuntimeManager {

  private static IdeRuntimeManager INSTANCE = new JdkManager();

  private JdkManager() {
  }

  @NotNull
  public static IdeRuntimeManager getInstance() {
    return INSTANCE;
  }

  @NotNull
  @Override
  public IdeRuntime createRuntime(@NotNull File runtimeDir) throws IOException {
    return new Jdk(runtimeDir);
  }
}
