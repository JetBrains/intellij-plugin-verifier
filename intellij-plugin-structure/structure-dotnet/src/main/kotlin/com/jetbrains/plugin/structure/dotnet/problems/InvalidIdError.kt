/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.dotnet.problems

import com.jetbrains.plugin.structure.base.problems.PluginProblem

class InvalidIdError : PluginProblem() {
  override val level = Level.ERROR
  override val message = "The id parameter in metadata must consist of two parts (company and a plugin name) separated by dot."
}

class InvalidVersionError(version: String) : PluginProblem() {
  override val level = Level.ERROR
  override val message = "Package version $version doesn't represent valid NuGet version."
}

class NullIdDependencyError : PluginProblem() {
  override val level = Level.ERROR
  override val message = "The declared dependency must have an ID."
}

class InvalidDependencyVersionError(version: String, message: String) : PluginProblem() {
  override val level = Level.ERROR
  override val message = "Dependency version $version doesn't represent valid NuGet version: $message."
}