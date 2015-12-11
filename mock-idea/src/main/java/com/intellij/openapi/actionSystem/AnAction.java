package com.intellij.openapi.actionSystem;

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

}
