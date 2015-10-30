package mock.plugin;

import com.intellij.openapi.actionSystem.AnAction;

/**
 * @author Sergey Patrikeev
 */
public class OverrideFinalMethodProblem extends AnAction {
  @Override
  public boolean isEnabledInModalContext() {
    return super.isEnabledInModalContext();
  }
}
