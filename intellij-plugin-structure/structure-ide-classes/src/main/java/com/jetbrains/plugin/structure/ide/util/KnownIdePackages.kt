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