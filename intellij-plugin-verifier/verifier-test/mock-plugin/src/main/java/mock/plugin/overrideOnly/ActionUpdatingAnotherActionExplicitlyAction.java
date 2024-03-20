package mock.plugin.overrideOnly;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ActionUpdatingAnotherActionExplicitlyAction extends AnAction {

    /*expected(OVERRIDE_ONLY)
      Invocation of override-only method mock.plugin.overrideOnly.SimpleAction.update(AnActionEvent)

      Override-only method mock.plugin.overrideOnly.SimpleAction.update(AnActionEvent) is invoked in mock.plugin.overrideOnly.ActionUpdatingAnotherActionExplicitlyAction.actionPerformed(AnActionEvent) : void. This method is marked with @org.jetbrains.annotations.ApiStatus.OverrideOnly annotation, which indicates that the method must be only overridden but not invoked by client code. See documentation of the @ApiStatus.OverrideOnly for more info.
    */
    @Override
    protected void actionPerformed(AnActionEvent e) {
        // this should be prohibited as per IDEA-336988
        new SimpleAction().update(e);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {

    }
}
