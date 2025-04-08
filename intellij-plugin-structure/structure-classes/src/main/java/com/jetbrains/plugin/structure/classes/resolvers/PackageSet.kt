/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.classes.resolvers

/**
 * Set of packages and their super-packages.
 *
 * For example, if one adds a package `com/example/utils` into [PackageSet],
 * then packages `com`, `com/example` and `com/example/utils` will be added, too.
 */
class PackageSet {

  private val packages = hashSetOf<String>()

  /**
   * Adds all packages and super-packages of the specified class.
   *
   * If the class has default package, list `[""]` is added.
   */
  fun addPackagesOfClass(className: String) {
    val superPackage = StringBuilder()
    var defaultPackage = true
    for (c in className) {
      if (c == '/') {
        defaultPackage = false
        packages.add(superPackage.toString())
      }
      superPackage.append(c)
    }
    if (defaultPackage) {
      packages.add("")
    }
  }

  fun addPackages(packages: Set<String>) {
    this.packages.addAll(packages)
  }

  fun containsPackage(packageName: String) = packageName in packages

  fun getAllPackages(): Set<String> = packages


}