package com.jetbrains.plugin.structure.intellij.utils.xincludes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

public interface XIncludePathResolver {
  @NotNull
  URL resolvePath(@NotNull String relativePath, @Nullable String base);
}
