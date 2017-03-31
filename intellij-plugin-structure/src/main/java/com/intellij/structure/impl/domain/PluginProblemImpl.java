package com.intellij.structure.impl.domain;

import com.intellij.structure.domain.PluginProblem;
import org.jetbrains.annotations.NotNull;

public class PluginProblemImpl implements PluginProblem {
  @NotNull private final Level myLevel;
  @NotNull private final String myMessage;

  public PluginProblemImpl(@NotNull String message, @NotNull Level level) {
    myLevel = level;
    myMessage = message;
  }

  @Override
  public Level getLevel() {
    return myLevel;
  }

  @Override
  public String getMessage() {
    return myMessage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PluginProblemImpl that = (PluginProblemImpl) o;

    if (myLevel != that.myLevel) return false;
    return myMessage.equals(that.myMessage);
  }

  @Override
  public int hashCode() {
    int result = myLevel.hashCode();
    result = 31 * result + myMessage.hashCode();
    return result;
  }
}
