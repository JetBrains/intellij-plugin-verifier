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
      if (base != null) {
        return new URL(new URL(base), relativePath);
      } else {
        return new URL(relativePath);
      }
    } catch (MalformedURLException ex) {
      throw new XIncludeException(ex);
    }
  }
}
