package com.jetbrains.plugin.structure.intellij.utils.xincludes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;

final public class DefaultXIncludePathResolver implements XIncludePathResolver {

  public static final DefaultXIncludePathResolver INSTANCE = new DefaultXIncludePathResolver();

  private DefaultXIncludePathResolver() {
  }

  @NotNull
  @Override
  public URL resolvePath(@NotNull String relativePath, @Nullable String base) {
    try {
      if (base != null) {
        if (relativePath.startsWith("/")) {
          relativePath = ".." + relativePath;
        }
        return new URL(new URL(base), relativePath);
      } else {
        return new URL(relativePath);
      }
    } catch (MalformedURLException e) {
      throw new XIncludeException(e);
    }
  }
}
