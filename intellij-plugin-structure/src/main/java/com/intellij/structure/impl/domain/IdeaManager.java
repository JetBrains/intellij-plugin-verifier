package com.intellij.structure.impl.domain;

import com.intellij.structure.domain.Ide;
import com.intellij.structure.domain.IdeManager;
import com.intellij.structure.domain.IdeRuntime;
import com.intellij.structure.pool.ClassPool;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * @author Sergey Patrikeev
 */
public class IdeaManager extends IdeManager {

  @NotNull
  @Override
  public Ide createIde(@NotNull File idePath, @NotNull IdeRuntime javaRuntime) throws IOException, JDOMException {
    return new Idea(idePath, javaRuntime);
  }

  @NotNull
  @Override
  public Ide createIde(@NotNull File idePath, @NotNull IdeRuntime ideRuntime, @Nullable ClassPool externalClasspath) throws IOException, JDOMException {
    return new Idea(idePath, ideRuntime, externalClasspath);
  }

}
