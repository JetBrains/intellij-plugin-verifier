package com.intellij.openapi.actionSystem;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ActionGroup extends AnAction {
    // Method has an explicit `OverrideOnly` annotation to be aligned with IDEA-336988
    @ApiStatus.OverrideOnly
    public abstract AnAction @NotNull [] getChildren(@Nullable AnActionEvent e);
}


