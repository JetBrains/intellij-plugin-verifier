package com.intellij.structure.impl.domain;

import com.intellij.structure.domain.IdeVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Sergey Patrikeev
 */
public class EmptyIdeVersion extends IdeVersion {

  public static final IdeVersion INSTANCE = new EmptyIdeVersion();

  private EmptyIdeVersion() {
  }

  @NotNull
  @Override
  public String getProductCode() {
    return "";
  }

  @Nullable
  @Override
  public String getProductName() {
    return null;
  }

  @Override
  public int getBranch() {
    return 0;
  }

  @Override
  public int getBuild() {
    return 0;
  }

  @Override
  public int getAttempt() {
    return 0;
  }

  @Override
  public boolean isSnapshot() {
    return false;
  }
}
