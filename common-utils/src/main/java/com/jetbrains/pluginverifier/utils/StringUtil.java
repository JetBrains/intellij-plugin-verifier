package com.jetbrains.pluginverifier.utils;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class StringUtil {

  @NotNull
  public static String repeatSymbol(final char aChar, final int count) {
    char[] buffer = new char[count];
    Arrays.fill(buffer, aChar);
    return new String(buffer);
  }

  public static String pluralize(@NotNull String s, int cnt) {
    return s + (cnt > 1 ? "s" : "");
  }

  @NotNull
  public static String trimStart(@NotNull String s, @NonNls @NotNull String prefix) {
    if (s.startsWith(prefix)) {
      return s.substring(prefix.length());
    }
    return s;
  }

  @NotNull
  public static String trimEnd(@NotNull String s, @NonNls @NotNull String suffix) {
    if (s.endsWith(suffix)) {
      return s.substring(0, s.length() - suffix.length());
    }
    return s;
  }

  @NotNull
  public static String commonPrefix(@NotNull String s1, @NotNull String s2) {
    return s1.substring(0, commonPrefixLength(s1, s2));
  }

  public static int commonPrefixLength(@NotNull CharSequence s1, @NotNull CharSequence s2) {
    int i;
    int minLength = Math.min(s1.length(), s2.length());
    for (i = 0; i < minLength; i++) {
      if (s1.charAt(i) != s2.charAt(i)) {
        break;
      }
    }
    return i;
  }

  public static boolean equals(@Nullable String s1, @Nullable String s2) {
    if (s1 == null ^ s2 == null) {
      return false;
    }

    if (s1 == null) {
      return true;
    }

    if (s1.length() != s2.length()) {
      return false;
    }
    for (int i = 0; i < s1.length(); i++) {
      if (s1.charAt(i) != s2.charAt(i)) {
        return false;
      }
    }
    return true;
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
