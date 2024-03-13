package mock.plugin.overrideOnly;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class DelegatingAction extends BaseAction {
    private final AnAction delegateAction = new SimpleAction();

    @SuppressWarnings("RedundantMethodOverride")
    @Override
    protected void actionPerformed(AnActionEvent e) {
        // no-op
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        delegateAction.update(e);
    }
}
