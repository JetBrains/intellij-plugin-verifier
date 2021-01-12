/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.ide.diff.builder.filter

class PackagesClassFilter(private val interestingPackages: List<String>) : ClassFilter {

  override fun shouldProcessClass(className: String): Boolean {
    val packageName = className.substringBeforeLast("/", "").replace('/', '.')
    if (knownObfuscatedPackages.any { packageName == it || packageName.startsWith("$it.") }) {
      return false
    }
    return interestingPackages.isEmpty() || interestingPackages.any { it.isEmpty() || packageName == it || packageName.startsWith("$it.") }
  }

  override fun toString(): String =
    if (interestingPackages.any { it.isEmpty() }) {
      "All packages"
    } else {
      "The following packages: [" + interestingPackages.joinToString() + "]"
    }

  companion object {
    private val knownObfuscatedPackages = listOf(
      "a",
      "b",
      "com.intellij.a",
      "com.intellij.b",
      "com.intellij.ide.a",
      "com.intellij.ide.b",
      "com.jetbrains.a",
      "com.jetbrains.b",
      "com.jetbrains.ls"
    )
  }

}