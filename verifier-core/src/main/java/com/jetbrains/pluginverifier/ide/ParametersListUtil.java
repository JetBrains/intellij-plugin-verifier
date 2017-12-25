/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.pluginverifier.ide;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public class ParametersListUtil {
  //public static final Function<String, List<String>> DEFAULT_LINE_PARSER = new Function<String, List<String>>() {
  //  @Override
  //  public List<String> fun(String text) {
  //    return parse(text, true);
  //  }
  //};
  //public static final Function<List<String>,String> DEFAULT_LINE_JOINER = new Function<List<String>, String>() {
  //  @Override
  //  public String fun(List<String> strings) {
  //    return StringUtil.join(strings, " ");
  //  }
  //};

  /**
   * <p>Joins list of parameters into single string, which may be then parsed back into list by parseToArray(String).</p>
   * <p/>
   * <p>
   * <strong>Conversion rules:</strong>
   * <ul>
   * <li>double quotes are escaped by backslash (<code>&#92;</code>);</li>
   * <li>empty parameters parameters and parameters with spaces inside are surrounded with double quotes (<code>"</code>);</li>
   * <li>parameters are separated by single whitespace.</li>
   * </ul>
   * </p>
   * <p/>
   * <p><strong>Examples:</strong></p>
   * <p>
   * <code>['a', 'b'] => 'a  b'</code><br/>
   * <code>['a="1 2"', 'b'] => '"a &#92;"1 2&#92;"" b'</code>
   * </p>
   *
   * @param parameters a list of parameters to join.
   * @return a string with parameters.
   */
  @NotNull
  public static String join(@NotNull final List<String> parameters) {
    return encode(parameters);
  }

  /**
   * <p>Splits single parameter string (as created by {@link #join(java.util.List)}) into list of parameters.</p>
   * <p/>
   * <p>
   * <strong>Conversion rules:</strong>
   * <ul>
   * <li>starting/whitespaces are trimmed;</li>
   * <li>parameters are split by whitespaces, whitespaces itself are dropped</li>
   * <li>parameters inside double quotes (<code>"a b"</code>) are kept as single one;</li>
   * <li>double quotes are dropped, escaped double quotes (<code>&#92;"</code>) are un-escaped.</li>
   * </ul>
   * </p>
   * <p/>
   * <p><strong>Examples:</strong></p>
   * <p>
   * <code>' a  b ' => ['a', 'b']</code><br/>
   * <code>'a="1 2" b' => ['a=1 2', 'b']</code><br/>
   * <code>'a " " b' => ['a', ' ', 'b']</code><br/>
   * <code>'"a &#92;"1 2&#92;"" b' => ['a="1 2"', 'b']</code>
   * </p>
   *
   * @param parameterString parameter string to split.
   * @return array of parameters.
   */
  @NotNull
  public static List<String> parse(@NotNull String parameterString) {
    return parse(parameterString, false);
  }

  @NotNull
  private static List<String> parse(@NotNull String parameterString, boolean keepQuotes) {
    parameterString = parameterString.trim();

    final ArrayList<String> params = new ArrayList<String>();
    final StringBuilder token = new StringBuilder(128);
    boolean inQuotes = false;
    boolean escapedQuote = false;
    boolean nonEmpty = false;

    for (int i = 0; i < parameterString.length(); i++) {
      final char ch = parameterString.charAt(i);

      if (ch == '\"') {
        if (!escapedQuote) {
          inQuotes = !inQuotes;
          nonEmpty = true;
          if (!keepQuotes) {
            continue;
          }
        }
        escapedQuote = false;
      } else if (Character.isWhitespace(ch)) {
        if (!inQuotes) {
          if (token.length() > 0 || nonEmpty) {
            params.add(token.toString());
            token.setLength(0);
            nonEmpty = false;
          }
          continue;
        }
      } else if (ch == '\\') {
        if (i < parameterString.length() - 1 && parameterString.charAt(i + 1) == '"') {
          escapedQuote = true;
          if (!keepQuotes) {
            continue;
          }
        }
      }

      token.append(ch);
    }

    if (token.length() > 0 || nonEmpty) {
      params.add(token.toString());
    }

    return params;
  }

  @NotNull
  private static String encode(@NotNull final List<String> parameters) {
    final StringBuilder buffer = new StringBuilder();
    for (final String parameter : parameters) {
      if (buffer.length() > 0) {
        buffer.append(' ');
      }
      buffer.append(encode(parameter));
    }
    return buffer.toString();
  }

  @NotNull
  private static String encode(@NotNull String parameter) {
    final StringBuilder builder = new StringBuilder();
    builder.append(parameter);
    escapeQuotes(builder);
    if (builder.length() == 0 || indexOf(builder, ' ') >= 0 || indexOf(builder, '|') >= 0) {
      quote(builder);
    }
    return builder.toString();
  }

  private static void quote(@NotNull final StringBuilder builder) {
    quote(builder, '\"');
  }

  private static void quote(@NotNull final StringBuilder builder, final char quotingChar) {
    builder.insert(0, quotingChar);
    builder.append(quotingChar);
  }

  private static void escapeQuotes(@NotNull final StringBuilder buf) {
    escapeChar(buf, '"');
  }

  private static void escapeChar(@NotNull final StringBuilder buf, final char character) {
    int idx = 0;
    while ((idx = indexOf(buf, character, idx)) >= 0) {
      buf.insert(idx, "\\");
      idx += 2;
    }
  }

  private static int indexOf(@NotNull CharSequence s, char c) {
    return indexOf(s, c, 0, s.length());
  }

  private static int indexOf(@NotNull CharSequence s, char c, int start) {
    return indexOf(s, c, start, s.length());
  }

  private static int indexOf(@NotNull CharSequence s, char c, int start, int end) {
    for (int i = start; i < end; i++) {
      if (s.charAt(i) == c) return i;
    }
    return -1;
  }
}
