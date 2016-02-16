package com.intellij.structure.impl.domain;

import com.intellij.structure.domain.IdeRuntimeManager;
import com.intellij.structure.domain.Jdk;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * @author Sergey Patrikeev
 */
public class JdkManager extends IdeRuntimeManager {

  @NotNull
  @Override
  public Jdk createRuntime(@NotNull File runtimeDir) throws IOException {
    return new com.intellij.structure.impl.domain.Jdk(runtimeDir);
  }
}
