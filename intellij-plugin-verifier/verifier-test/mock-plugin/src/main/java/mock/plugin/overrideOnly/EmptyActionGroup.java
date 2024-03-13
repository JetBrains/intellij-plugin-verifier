package mock.plugin.overrideOnly;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EmptyActionGroup extends ActionGroup {
    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        return new AnAction[0];
    }

    @Override
    protected void actionPerformed(AnActionEvent e) {
        // no-op
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // no-op
    }
}
