package mock.plugin.overrideOnly;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ActionGroupUpdatingAnotherActionActionGroup extends ActionGroup {
    /*expected(OVERRIDE_ONLY)
    Invocation of override-only method mock.plugin.overrideOnly.SimpleAction.update(AnActionEvent)

    Override-only method mock.plugin.overrideOnly.SimpleAction.update(AnActionEvent) is invoked in mock.plugin.overrideOnly.ActionGroupUpdatingAnotherActionActionGroup.actionPerformed(AnActionEvent) : void. This method is marked with @org.jetbrains.annotations.ApiStatus.OverrideOnly annotation, which indicates that the method must be only overridden but not invoked by client code. See documentation of the @ApiStatus.OverrideOnly for more info.
    */
    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        return new AnAction[0];
    }

    @Override
    protected void actionPerformed(AnActionEvent e) {
        // this should be prohibited as per IDEA-336988
        new SimpleAction().update(e);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // no-op
    }
}
