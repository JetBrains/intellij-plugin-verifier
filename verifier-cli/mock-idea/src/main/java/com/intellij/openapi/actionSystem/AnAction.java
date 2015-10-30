package com.intellij.openapi.actionSystem;

public abstract class AnAction {

  public static void nonExistingMethod() {
  }

  //actually this method is Final => OverrideFinalProblem
  public boolean isEnabledInModalContext() {
    return false;
  }

  /**
   * Override with true returned if your action has to display its text along with the icon when placed in the toolbar
   */
  public boolean displayTextInToolbar() {
    return false;
  }

}
