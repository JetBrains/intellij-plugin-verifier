package com.intellij.structure.impl.utils;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;

public class StringUtil {

  public static boolean isNullOrEmpty(@Nullable String s) {
    return s == null || s.isEmpty();
  }

  @NotNull
  public static String notNullize(@Nullable String s) {
    return s == null ? "" : s;
  }

  public static int countChars(@NotNull CharSequence text, char c) {
    return countChars(text, c, 0, false);
  }

  public static int countChars(@NotNull CharSequence text, char c, int offset, boolean continuous) {
    int count = 0;
    for (int i = offset; i < text.length(); ++i) {
      if (text.charAt(i) == c) {
        count++;
      } else if (continuous) {
        break;
      }
    }
    return count;
  }

  public static boolean endsWithIgnoreCase(@NonNls @NotNull String text, @NonNls @NotNull String suffix) {
    int l1 = text.length();
    int l2 = suffix.length();
    if (l1 < l2) return false;

    for (int i = l1 - 1; i >= l1 - l2; i--) {
      if (!charsEqualIgnoreCase(text.charAt(i), suffix.charAt(i + l2 - l1))) {
        return false;
      }
    }

    return true;

  }

  private static boolean charsEqualIgnoreCase(char a, char b) {
    return a == b || toUpperCase(a) == toUpperCase(b) || toLowerCase(a) == toLowerCase(b);
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

  public static boolean isEmpty(@Nullable String s) {
    return s == null || s.isEmpty();
  }
}
