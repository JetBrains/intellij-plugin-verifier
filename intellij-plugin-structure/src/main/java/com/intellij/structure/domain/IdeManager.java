package com.intellij.structure.domain;

import com.intellij.structure.impl.domain.IdeaManager;
import com.intellij.structure.pool.ClassPool;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * @author Sergey Patrikeev
 */
public abstract class IdeManager {

  private static IdeManager ourInstance;

  public static IdeManager getInstance() {
    if (ourInstance == null) {
      //change if necessary
      ourInstance = new IdeaManager();
    }
    return ourInstance;
  }


  @NotNull
  public Ide createIde(@NotNull File idePath, @NotNull IdeRuntime javaRuntime) throws IOException, JDOMException {
    return createIde(idePath, javaRuntime, null);
  }

  @NotNull
  public abstract Ide createIde(@NotNull File idePath, @NotNull IdeRuntime ideRuntime, @Nullable ClassPool externalClasspath) throws IOException, JDOMException;

}
