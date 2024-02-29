package mock.plugin.overrideOnly;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class DelegatingAction extends AnAction {
    private final AnAction delegateAction = new SimpleAction();

    @Override
    protected void actionPerformed(AnActionEvent e) {
        // no-op
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        delegateAction.update(e);
    }
}
