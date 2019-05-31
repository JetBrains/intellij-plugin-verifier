package com.jetbrains.plugin.structure.intellij.utils.xincludes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;

public class DefaultXIncludePathResolver implements XIncludePathResolver {
  @NotNull
  @Override
  public URL resolvePath(@NotNull String relativePath, @Nullable String base) {
    try {
      return base == null ? new URL(relativePath) : new URL(new URL(base), relativePath);
    } catch (MalformedURLException ex) {
      throw new XIncludeException(ex);
    }
  }
}
