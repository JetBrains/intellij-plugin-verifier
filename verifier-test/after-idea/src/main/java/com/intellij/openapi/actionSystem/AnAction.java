package com.intellij.openapi.actionSystem;

public abstract class AnAction {

  public final boolean isEnabledInModalContext() {
    return false;
  }

  public boolean displayTextInToolbar() {
    return false;
  }

  protected abstract void actionPerformed(AnActionEvent e);

}
