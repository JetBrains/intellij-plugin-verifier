package com.jetbrains.pluginverifier.utils;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StringUtil {

  @NotNull
  public static String replace(@NonNls @NotNull String text, @NonNls @NotNull String oldS, @NonNls @Nullable String newS) {
    return replace(text, oldS, newS, false);
  }

  public static String replace(@NotNull final String text, @NotNull final String oldS, @Nullable final String newS, boolean ignoreCase) {
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
      }
      else {
        if (newS == null) return null;
        if (newText == null) newText = new StringBuilder(text.length() - i);
        newText.append(text, i, i1);
        newText.append(newS);
        i = i1 + oldS.length();
      }
    }
    return newText != null ? newText.toString() : "";
  }

  public static boolean isEmpty(@Nullable String s) {
    return s == null || s.isEmpty();
  }

  public static boolean isNotEmpty(@Nullable String s) {
    return s != null && !s.isEmpty();
  }

  public static void escapeQuotes(@NotNull final StringBuilder buf) {
    escapeChar(buf, '"');
  }

  private static void escapeChar(@NotNull final StringBuilder buf, final char character) {
    int idx = 0;
    while ((idx = indexOf(buf, character, idx)) >= 0) {
      buf.insert(idx, "\\");
      idx += 2;
    }
  }

  public static int indexOf(@NotNull CharSequence s, char c) {
    return indexOf(s, c, 0, s.length());
  }

  public static int indexOf(@NotNull CharSequence s, char c, int start) {
    return indexOf(s, c, start, s.length());
  }

  public static int indexOf(@NotNull CharSequence s, char c, int start, int end) {
    for (int i = start; i < end; i++) {
      if (s.charAt(i) == c) return i;
    }
    return -1;
  }

  public static void quote(@NotNull final StringBuilder builder) {
    quote(builder, '\"');
  }

  public static void quote(@NotNull final StringBuilder builder, final char quotingChar) {
    builder.insert(0, quotingChar);
    builder.append(quotingChar);
  }

}
