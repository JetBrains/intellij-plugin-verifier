package com.jetbrains.pluginverifier.utils;

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

  /**
   * @param methodDescr example:
   *                    com/intellij/codeInsight/intention/ConditionalOperatorConvertor#isAvailable(Lcom/intellij/openapi/project/Project;Lcom/intellij/openapi/editor/Editor;Lcom/intellij/psi/PsiFile;)Z
   */
  public static String convertMethodDescr(String methodDescr) {
    int classSeparatorIndex = methodDescr.indexOf('#');
    if (classSeparatorIndex == -1) return methodDescr;

    int openBracketIndex = methodDescr.indexOf('(');
    if (openBracketIndex == -1) return methodDescr;

    int closeBracketIndex = methodDescr.lastIndexOf(')');

    StringBuilder res = new StringBuilder();

    int i = processJavaType(res, methodDescr, closeBracketIndex + 1);

    res.append(' ');

    res.append(methodDescr.substring(0, classSeparatorIndex).replace('/', '.'));

    res.append(methodDescr, classSeparatorIndex, openBracketIndex + 1);

    i = openBracketIndex + 1;
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
