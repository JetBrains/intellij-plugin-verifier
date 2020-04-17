/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.util

/**
 * Utility class that contains list of known IntelliJ IDE packages.
 */
object KnownIdePackages {

  private val idePackages: Set<String> by lazy(::readKnownIdePackages)

  fun isKnownPackage(packageName: String): Boolean =
    idePackages.any { packageName == it || packageName.startsWith("$it.") }

  private fun readKnownIdePackages() =
    KnownIdePackages::class.java.classLoader
      .getResourceAsStream("com.jetbrains.plugin.structure.ide/knownIdePackages.txt")!!
      .bufferedReader()
      .readLines()
      .toSet()
}