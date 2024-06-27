package com.intellij.openapi.actionSystem;

import com.jetbrains.pluginverifier.verifiers.resolution.DisableSameModuleInvocations;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public abstract class AnAction {

  public static void nonExistingMethod() {
  }

  //actually this method is Final => OverrideFinalProblem
  public boolean isEnabledInModalContext() {
    return false;
  }

  public boolean displayTextInToolbar() {
    return false;
  }

  protected abstract void actionPerformed(AnActionEvent e);

  @ApiStatus.OverrideOnly
  @DisableSameModuleInvocations
  public abstract void update(@NotNull AnActionEvent e);
}
