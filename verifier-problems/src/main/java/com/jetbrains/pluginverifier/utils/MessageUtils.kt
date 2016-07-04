package com.jetbrains.pluginverifier.utils

import com.google.common.base.Preconditions
import java.util.*
import java.util.regex.Pattern

object MessageUtils {

  private val CLASS_QUALIFIED_NAME = Pattern.compile("(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(?:\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*)\\.(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)")

  private val COMMON_CLASSES = HashSet(Arrays.asList("com.intellij.openapi.vfs.VirtualFile",
      "com.intellij.openapi.module.Module",
      "com.intellij.openapi.project.Project",
      "com.intellij.psi.PsiElement",
      "com.intellij.openapi.util.TextRange",
      "com.intellij.psi.PsiFile",
      "com.intellij.psi.PsiReference",
      "com.intellij.psi.search.GlobalSearchScope",
      "com.intellij.lang.ASTNode"))
  private val COMMON_PACKAGES = HashSet(Arrays.asList("java.io", "java.lang", "java.util"))

  fun convertClassName(className: String): String {
    return className.replace('/', '.')
  }

  private fun processJavaType(res: StringBuilder, s: String, start: Int): Int {
    var start = start
    var arrayDeep = 0

    while (s.startsWith("[", start)) {
      arrayDeep++
      start++
    }

    val text: String

    var end = start + 1

    when (s[start]) {
      'L' -> {
        val p = s.indexOf(';', start)
        if (p == -1) {
          throw IllegalArgumentException(s.substring(start))
        }

        text = s.substring(start + 1, p).replace('/', '.')
        end = p + 1
      }

      'V' -> text = "void"

      'B' -> text = "byte"
      'C' -> text = "char"
      'D' -> text = "double"
      'F' -> text = "float"
      'I' -> text = "int"
      'J' -> text = "long"
      'S' -> text = "short"
      'Z' -> text = "boolean"

      else -> throw IllegalArgumentException(s.substring(start))
    }

    res.append(text)

    for (i in 0..arrayDeep - 1) {
      res.append("[]")
    }

    return end
  }

  fun convertMethodDescr(methodDescr: String): String {
    return convertMethodDescr0(methodDescr, null)
  }

  /**
   * @param methodDescr example: com/intellij/codeInsight/intention/ConditionalOperatorConvertor#isAvailable(Lcom/intellij/openapi/project/Project;Lcom/intellij/openapi/editor/Editor;Lcom/intellij/psi/PsiFile;)Z
   */
  fun convertMethodDescr(methodDescr: String, className: String): String {
    return convertMethodDescr0(methodDescr, className)
  }

  private fun convertMethodDescr0(methodDescr: String, className: String?): String {
    var className = className
    val closeBracketIndex = methodDescr.lastIndexOf(')')

    val openBracketIndex = methodDescr.indexOf('(')
    if (openBracketIndex == -1) return methodDescr

    val res = StringBuilder()

    processJavaType(res, methodDescr, closeBracketIndex + 1)
    res.append(' ')

    val methodNameIndex: Int

    if (className != null) {
      Preconditions.checkArgument(methodDescr.indexOf('#') == -1)
      methodNameIndex = 0
    } else {
      val classSeparatorIndex = methodDescr.indexOf('#')
      if (classSeparatorIndex == -1) return methodDescr
      className = methodDescr.substring(0, classSeparatorIndex)

      methodNameIndex = classSeparatorIndex + 1
    }

    res.append(className.replace('/', '.'))
    res.append('#')

    res.append(methodDescr, methodNameIndex, openBracketIndex + 1)

    var i = openBracketIndex + 1
    var isFirst = true
    while (i < closeBracketIndex) {
      if (isFirst) {
        isFirst = false
      } else {
        res.append(", ")
      }

      i = processJavaType(res, methodDescr, i)
    }

    res.append(')')

    return res.toString()
  }

  fun cutCommonPackages(text: String): String {
    val matcher = CLASS_QUALIFIED_NAME.matcher(text)

    if (!matcher.find()) return text

    var idx = 0
    val res = StringBuilder()

    do {
      res.append(text, idx, matcher.start())

      if (COMMON_CLASSES.contains(matcher.group()) || COMMON_PACKAGES.contains(matcher.group(1))) {
        res.append(matcher.group(2)) // class name without package
      } else {
        res.append(matcher.group()) // qualified class name
      }

      idx = matcher.end()
    } while (matcher.find())

    res.append(text, idx, text.length)

    return res.toString()
  }
}
