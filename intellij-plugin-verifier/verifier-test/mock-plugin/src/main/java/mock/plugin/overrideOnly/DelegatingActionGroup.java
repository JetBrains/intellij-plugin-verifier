package mock.plugin.overrideOnly;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DelegatingActionGroup extends ActionGroup {
    private final ActionGroup delegate = new EmptyActionGroup();

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        return delegate.getChildren(e);
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
