package com.jetbrains.pluginverifier.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MessageUtils {

  private MessageUtils() {
  }

  public static String convertClassName(String className) {
    return className.replace('/', '.');
  }

  private static int processJavaType(StringBuilder res, String s, int start) {
    int arrayDeep = 0;

    while (s.startsWith("[", start)) {
      arrayDeep++;
      start++;
    }

    String text;

    int end = start + 1;

    switch (s.charAt(start)) {
      case 'L':
        int p = s.indexOf(';', start);
        if (p == -1) {
          throw new IllegalArgumentException(s.substring(start));
        }

        text = s.substring(start + 1, p).replace('/', '.');
        end = p + 1;
        break;

      case 'V':
        text = "void";
        break;

      case 'B':
        text = "byte";
        break;
      case 'C':
        text = "char";
        break;
      case 'D':
        text = "double";
        break;
      case 'F':
        text = "float";
        break;
      case 'I':
        text = "int";
        break;
      case 'J':
        text = "long";
        break;
      case 'S':
        text = "short";
        break;
      case 'Z':
        text = "boolean";
        break;

      default:
        throw new IllegalArgumentException(s.substring(start));
    }

    res.append(text);

    for (int i = 0; i < arrayDeep; i++) {
      res.append("[]");
    }

    return end;
  }

  public static String convertMethodDescr(String methodDescr) {
    return convertMethodDescr0(methodDescr, null);
  }

  /**
   * @param methodDescr example:
   *                    com/intellij/codeInsight/intention/ConditionalOperatorConvertor#isAvailable(Lcom/intellij/openapi/project/Project;Lcom/intellij/openapi/editor/Editor;Lcom/intellij/psi/PsiFile;)Z
   */
  public static String convertMethodDescr(String methodDescr, @NotNull String className) {
    return convertMethodDescr0(methodDescr, className);
  }

  public static String convertMethodDescr0(String methodDescr, @Nullable String className) {
    int closeBracketIndex = methodDescr.lastIndexOf(')');

    int openBracketIndex = methodDescr.indexOf('(');
    if (openBracketIndex == -1) return methodDescr;

    StringBuilder res = new StringBuilder();

    processJavaType(res, methodDescr, closeBracketIndex + 1);
    res.append(' ');

    int methodNameIndex;

    if (className != null) {
      assert methodDescr.indexOf('#') == -1;
      methodNameIndex = 0;
    }
    else {
      int classSeparatorIndex = methodDescr.indexOf('#');
      if (classSeparatorIndex == -1) return methodDescr;
      className = methodDescr.substring(0, classSeparatorIndex);

      methodNameIndex = classSeparatorIndex + 1;
    }

    res.append(className.replace('/', '.'));
    res.append('#');

    res.append(methodDescr, methodNameIndex, openBracketIndex + 1);

    int i = openBracketIndex + 1;
    boolean isFirst = true;
    while (i < closeBracketIndex) {
      if (isFirst) {
        isFirst = false;
      }
      else {
        res.append(", ");
      }

      i = processJavaType(res, methodDescr, i);
    }

    res.append(')');

    return res.toString();
  }


}
