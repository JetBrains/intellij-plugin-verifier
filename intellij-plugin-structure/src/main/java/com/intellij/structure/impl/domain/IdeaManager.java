package com.intellij.structure.impl.domain;

import com.intellij.structure.domain.Ide;
import com.intellij.structure.domain.IdeManager;
import com.intellij.structure.errors.IncorrectPluginException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * @author Sergey Patrikeev
 */
public class IdeaManager extends IdeManager {

  private static final IdeaManager INSTANCE = new IdeaManager();

  private IdeaManager() {
  }

  public static IdeaManager getInstance() {
    return INSTANCE;
  }


  @NotNull
  @Override
  public Ide createIde(@NotNull File idePath) throws IOException, IncorrectPluginException {
    return new Idea(idePath);
  }
}
