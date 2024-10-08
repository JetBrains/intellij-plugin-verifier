/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.results.problems

data class UndeclaredPluginDependencyProblem(
  val undeclaredPluginId: String,
  val classOrPackage: ApiElement,
  val reason: String?
) : CompatibilityProblem() {
  override val problemType: String = "Undeclared plugin dependency"
  override val shortDescription: String =
    "Plugin '$undeclaredPluginId' not declared as a plugin dependency for $classOrPackage" + reason?.let { ". $it" }
      .orEmpty()
  override val fullDescription: String =
    "Plugin '$undeclaredPluginId' is not declared in the plugin descriptor as a dependency for $classOrPackage" + reason?.let { ". $it" }
      .orEmpty()

  sealed class ApiElement {
    data class Class(val className: String) : ApiElement() {
      override fun toString(): String = "class [$className]"
    }

    data class Package(val packageName: String) : ApiElement() {
      override fun toString(): String = "package [$packageName]"
    }
  }
}