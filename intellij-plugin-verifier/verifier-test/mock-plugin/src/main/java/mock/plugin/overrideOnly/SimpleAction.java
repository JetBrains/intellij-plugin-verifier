package mock.plugin.overrideOnly;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class SimpleAction extends BaseAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
    }
}
