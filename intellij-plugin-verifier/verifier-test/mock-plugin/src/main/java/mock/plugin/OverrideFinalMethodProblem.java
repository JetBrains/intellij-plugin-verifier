package mock.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class OverrideFinalMethodProblem extends AnAction {
  /*expected(PROBLEM)
    Overriding a final method com.intellij.openapi.actionSystem.AnAction.isEnabledInModalContext() : boolean

    Class mock.plugin.OverrideFinalMethodProblem overrides the final method com.intellij.openapi.actionSystem.AnAction.isEnabledInModalContext() : boolean. This can lead to **VerifyError** exception at runtime.
    */
  @Override
  public boolean isEnabledInModalContext() {
    return super.isEnabledInModalContext();
  }

  //problem shouldn't be found here
  protected void actionPerformed(AnActionEvent e) {

  }

  // just a plain override, no issues here
  @Override
  public void update(@NotNull AnActionEvent e) {

  }
}
