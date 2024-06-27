package mock.plugin.overrideOnly;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public class ActionWithOverrideOnlyMethod extends AnAction {
    @Override
    protected void actionPerformed(AnActionEvent e) {
        // This is allowed as the caller (ActionWithOverrideOnlyMethod)
        // and the callee (Executor) are effectively in the same module/JAR
        new Executor().execute();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // NO-OP
    }

    @ApiStatus.OverrideOnly
    public void handle() {
        // NO-OP
    }

    static class Executor {
        @ApiStatus.OverrideOnly
        void execute() {
            // some dummy code
            Instant.now();
        }
    }
}
