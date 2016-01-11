package com.intellij.structure.domain;

import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.pool.ClassPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * @author Sergey Patrikeev
 */
public abstract class IdeManager {

  @NotNull
  public Ide createIde(@NotNull File idePath, @NotNull IdeRuntime javaRuntime) throws IOException, IncorrectPluginException {
    return createIde(idePath, javaRuntime, null);
  }

  @NotNull
  public abstract Ide createIde(@NotNull File idePath, @NotNull IdeRuntime ideRuntime, @Nullable ClassPool externalClasspath) throws IOException, IncorrectPluginException;

}
