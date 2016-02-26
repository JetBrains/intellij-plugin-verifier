package mock.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

/**
 * @author Sergey Patrikeev
 */
public class OverrideFinalMethodProblem extends AnAction {
  @Override
  public boolean isEnabledInModalContext() {
    return super.isEnabledInModalContext();
  }

  //problem shouldn't be found here
  protected void actionPerformed(AnActionEvent e) {

  }

}
