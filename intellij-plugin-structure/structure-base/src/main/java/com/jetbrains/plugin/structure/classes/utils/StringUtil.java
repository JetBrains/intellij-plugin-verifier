package com.jetbrains.plugin.structure.classes.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;

public class StringUtil {

  @Contract("null -> true")
  public static boolean isEmptyOrSpaces(@Nullable String s) {
    if (StringUtil.isEmpty(s)) {
      return true;
    }
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) > ' ') {
        return false;
      }
    }
    return true;
  }

  public static int numberOfPatternMatches(@NotNull String text, Pattern pattern) {
    Matcher matcher = pattern.matcher(text);
    int count = 0;
    while (matcher.find()) {
      count++;
    }
    return count;
  }

  @NotNull
  public static String toSystemIndependentName(@NonNls @NotNull String fileName) {
    return fileName.replace('\\', '/');
  }

  @NotNull
  public static String repeat(@NotNull String s, int count) {
    StringBuilder sb = new StringBuilder(s.length() * count);
    for (int i = 0; i < count; i++) {
      sb.append(s);
    }
    return sb.toString();
  }

  public static int countChars(@NotNull CharSequence text, char c) {
    return countChars(text, c, 0, false);
  }

  private static int countChars(@NotNull CharSequence text, char c, int offset, boolean continuous) {
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
  public static String trimLeading(@NotNull String string, char symbol) {
    int index = 0;
    while (index < string.length() && string.charAt(index) == symbol) index++;
    return string.substring(index);
  }

  @NotNull
  public static String trimStart(@NotNull String s, @NonNls @NotNull String prefix) {
    if (s.startsWith(prefix)) {
      return s.substring(prefix.length());
    }
    return s;
  }

  public static boolean containsIgnoreCase(@NotNull String where, @NotNull String what) {
    return indexOfIgnoreCase(where, what, 0) >= 0;
  }

  private static int indexOfIgnoreCase(@NotNull String where, @NotNull String what, int fromIndex) {
    int targetCount = what.length();
    int sourceCount = where.length();

    if (fromIndex >= sourceCount) {
      return targetCount == 0 ? sourceCount : -1;
    }

    if (fromIndex < 0) {
      fromIndex = 0;
    }

    if (targetCount == 0) {
      return fromIndex;
    }

    char first = what.charAt(0);
    int max = sourceCount - targetCount;

    for (int i = fromIndex; i <= max; i++) {
      /* Look for first character. */
      if (!charsEqualIgnoreCase(where.charAt(i), first)) {
        while (++i <= max && !charsEqualIgnoreCase(where.charAt(i), first)) ;
      }

      /* Found first character, now look at the rest of v2 */
      if (i <= max) {
        int j = i + 1;
        int end = j + targetCount - 1;
        for (int k = 1; j < end && charsEqualIgnoreCase(where.charAt(j), what.charAt(k)); j++, k++) ;

        if (j == end) {
          /* Found whole string. */
          return i;
        }
      }
    }

    return -1;
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
