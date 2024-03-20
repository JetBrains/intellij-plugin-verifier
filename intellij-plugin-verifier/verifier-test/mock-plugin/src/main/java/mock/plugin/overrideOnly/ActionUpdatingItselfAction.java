package mock.plugin.overrideOnly;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ActionUpdatingItselfAction extends AnAction {
    /*expected(OVERRIDE_ONLY)
    Invocation of override-only method mock.plugin.overrideOnly.ActionUpdatingItselfAction.update(AnActionEvent)

    Override-only method mock.plugin.overrideOnly.ActionUpdatingItselfAction.update(AnActionEvent) is invoked in mock.plugin.overrideOnly.ActionUpdatingItselfAction.update(AnActionEvent) : void. This method is marked with @org.jetbrains.annotations.ApiStatus.OverrideOnly annotation, which indicates that the method must be only overridden but not invoked by client code. See documentation of the @ApiStatus.OverrideOnly for more info.
    */
    @Override
    public void update(@NotNull AnActionEvent e) {
        ActionUpdatingItselfAction action = new ActionUpdatingItselfAction();
        // this should be prohibited as per IDEA-336988
        action.update(e);
    }

    @Override
    protected void actionPerformed(AnActionEvent e) {

    }
}
