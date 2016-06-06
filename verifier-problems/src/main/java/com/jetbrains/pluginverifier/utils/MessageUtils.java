package com.jetbrains.pluginverifier.utils;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {

  private static final Pattern CLASS_QUALIFIED_NAME = Pattern.compile("(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(?:\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*)\\.(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)");

  private static final Set<String> COMMON_CLASSES = new HashSet<String>(Arrays.asList("com.intellij.openapi.vfs.VirtualFile",
      "com.intellij.openapi.module.Module",
      "com.intellij.openapi.project.Project",
      "com.intellij.psi.PsiElement",
      "com.intellij.openapi.util.TextRange",
      "com.intellij.psi.PsiFile",
      "com.intellij.psi.PsiReference",
      "com.intellij.psi.search.GlobalSearchScope",
      "com.intellij.lang.ASTNode"));
  private static final Set<String> COMMON_PACKAGES = new HashSet<String>(Arrays.asList("java.io", "java.lang", "java.util"));

  private MessageUtils() {
  }

  public static String convertClassName(@NotNull String className) {
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

  public static String convertMethodDescr(@NotNull String methodDescr) {
    return convertMethodDescr0(methodDescr, null);
  }

  /**
   * @param methodDescr example: com/intellij/codeInsight/intention/ConditionalOperatorConvertor#isAvailable(Lcom/intellij/openapi/project/Project;Lcom/intellij/openapi/editor/Editor;Lcom/intellij/psi/PsiFile;)Z
   */
  public static String convertMethodDescr(@NotNull String methodDescr, @NotNull String className) {
    return convertMethodDescr0(methodDescr, className);
  }

  private static String convertMethodDescr0(@NotNull String methodDescr, @Nullable String className) {
    int closeBracketIndex = methodDescr.lastIndexOf(')');

    int openBracketIndex = methodDescr.indexOf('(');
    if (openBracketIndex == -1) return methodDescr;

    StringBuilder res = new StringBuilder();

    processJavaType(res, methodDescr, closeBracketIndex + 1);
    res.append(' ');

    int methodNameIndex;

    if (className != null) {
      Preconditions.checkArgument(methodDescr.indexOf('#') == -1);
      methodNameIndex = 0;
    } else {
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
      } else {
        res.append(", ");
      }

      i = processJavaType(res, methodDescr, i);
    }

    res.append(')');

    return res.toString();
  }

  public static CharSequence cutCommonPackages(@NotNull String text) {
    Matcher matcher = CLASS_QUALIFIED_NAME.matcher(text);

    if (!matcher.find()) return text;

    int idx = 0;
    StringBuilder res = new StringBuilder();

    do {
      res.append(text, idx, matcher.start());

      if (COMMON_CLASSES.contains(matcher.group()) || COMMON_PACKAGES.contains(matcher.group(1))) {
        res.append(matcher.group(2)); // class name without package
      } else {
        res.append(matcher.group()); // qualified class name
      }

      idx = matcher.end();
    } while (matcher.find());

    res.append(text, idx, text.length());

    return res;
  }
}
