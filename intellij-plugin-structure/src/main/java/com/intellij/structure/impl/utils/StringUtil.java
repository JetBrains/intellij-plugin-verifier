package com.intellij.structure.impl.utils;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;

public class StringUtil {

  @Nullable
  public static String substringAfter(@NotNull String text, @NotNull String subString) {
    int i = text.indexOf(subString);
    if (i == -1) return null;
    return text.substring(i + subString.length());
  }

  @Nullable
  public static String substringBefore(@NotNull String text, @NotNull String subString) {
    int i = text.indexOf(subString);
    if (i == -1) return null;
    return text.substring(0, i);
  }

  @Nullable
  public static String substringBeforeIncluding(@NotNull String text, @NotNull String subString) {
    String before = substringBefore(text, subString);
    if (before == null) return null;
    return before + subString;
  }

  @NotNull
  public static String getFileName(@NotNull String path) {
    if (path.length() == 0) return "";
    char c = path.charAt(path.length() - 1);
    int end = c == '/' || c == '\\' ? path.length() - 1 : path.length();
    int start = Math.max(path.lastIndexOf('/', end - 1), path.lastIndexOf('\\', end - 1)) + 1;
    return path.substring(start, end);
  }

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

  @NotNull
  public static List<String> split(@NotNull String s, @NotNull String separator) {
    return split(s, separator, true);
  }

  @NotNull
  public static List<String> split(@NotNull String s, @NotNull String separator,
                                   boolean excludeSeparator) {
    return split(s, separator, excludeSeparator, true);
  }

  @SuppressWarnings("unchecked")
  @NotNull
  public static List<String> split(@NotNull String s, @NotNull String separator,
                                   boolean excludeSeparator, boolean excludeEmptyStrings) {
    return (List) split((CharSequence) s, separator, excludeSeparator, excludeEmptyStrings);
  }

  @NotNull
  public static List<CharSequence> split(@NotNull CharSequence s, @NotNull CharSequence separator,
                                         boolean excludeSeparator, boolean excludeEmptyStrings) {
    if (separator.length() == 0) {
      return Collections.singletonList(s);
    }
    List<CharSequence> result = new ArrayList<CharSequence>();
    int pos = 0;
    while (true) {
      int index = indexOf(s, separator, pos);
      if (index == -1) break;
      final int nextPos = index + separator.length();
      CharSequence token = s.subSequence(pos, excludeSeparator ? index : nextPos);
      if (token.length() != 0 || !excludeEmptyStrings) {
        result.add(token);
      }
      pos = nextPos;
    }
    if (pos < s.length() || !excludeEmptyStrings && pos == s.length()) {
      result.add(s.subSequence(pos, s.length()));
    }
    return result;
  }

  public static int indexOf(@NotNull CharSequence sequence, @NotNull CharSequence infix, int start) {
    for (int i = start; i <= sequence.length() - infix.length(); i++) {
      if (startsWith(sequence, i, infix)) {
        return i;
      }
    }
    return -1;
  }

  public static boolean startsWith(@NotNull CharSequence text, int startIndex, @NotNull CharSequence prefix) {
    int l1 = text.length() - startIndex;
    int l2 = prefix.length();
    if (l1 < l2) return false;

    for (int i = 0; i < l2; i++) {
      if (text.charAt(i + startIndex) != prefix.charAt(i)) return false;
    }

    return true;
  }


  @NotNull
  public static String toSystemIndependentName(@NonNls @NotNull String fileName) {
    return fileName.replace('\\', '/');
  }

  @NotNull
  public static String toSystemDependentName(@NonNls @NotNull String fileName) {
    return fileName.replace('/', File.separatorChar).replace('\\', File.separatorChar);
  }

  @NotNull
  public static String notNullize(@Nullable String s) {
    return s == null ? "" : s;
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
