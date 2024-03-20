package mock.plugin.overrideOnly;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class BaseAction extends AnAction {
    @Override
    protected void actionPerformed(AnActionEvent e) {
        // no-op
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // no-op
    }
}
