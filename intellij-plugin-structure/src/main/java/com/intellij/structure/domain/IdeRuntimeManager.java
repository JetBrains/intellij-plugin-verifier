package com.intellij.structure.domain;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * @author Sergey Patrikeev
 */
public abstract class IdeRuntimeManager {

  @NotNull
  public abstract IdeRuntime createRuntime(@NotNull File runtimeDir) throws IOException;

}
