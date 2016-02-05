package com.intellij.structure.impl.utils;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StringUtil {

  @NotNull
  public static String notNullize(@Nullable String s) {
    return s == null ? "" : s;
  }

  @NotNull
  public static String trimEnd(@NotNull String s, @NonNls @NotNull String suffix) {
    if (s.endsWith(suffix)) {
      return s.substring(0, s.length() - suffix.length());
    }
    return s;
  }

  public static boolean equal(@Nullable String arg1, @Nullable String arg2) {
    return arg1 == null ? arg2 == null : arg1.equals(arg2);
  }

  @NotNull
  public static String replace(@NonNls @NotNull String text, @NonNls @NotNull String oldS, @NonNls @NotNull String newS) {
    return replace(text, oldS, newS, false);
  }

  @NotNull
  private static String replace(@NotNull String text, @NotNull String oldS, @NotNull String newS, boolean ignoreCase) {
    if (text.length() < oldS.length()) return text;

    final String text1 = ignoreCase ? text.toLowerCase() : text;
    final String oldS1 = ignoreCase ? oldS.toLowerCase() : oldS;
    StringBuilder newText = null;
    int i = 0;

    while (i < text1.length()) {
      int i1 = text1.indexOf(oldS1, i);
      if (i1 < 0) {
        if (i == 0) return text;
        newText.append(text, i, text.length());
        break;
      } else {
        if (newText == null) newText = new StringBuilder(text.length() - i);
        newText.append(text, i, i1);
        newText.append(newS);
        i = i1 + oldS.length();
      }
    }
    return newText != null ? newText.toString() : "";
  }
}
