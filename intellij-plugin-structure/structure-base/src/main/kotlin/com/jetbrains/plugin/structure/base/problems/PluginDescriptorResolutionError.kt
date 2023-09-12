/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.problems

abstract class PluginDescriptorResolutionError : PluginProblem() {
  override val level
    get() = Level.ERROR
}

class PluginDescriptorIsNotFound(private val descriptorPath: String) : PluginDescriptorResolutionError() {
  override val message
    get() = "The plugin descriptor '$descriptorPath' is not found."
}

class MultiplePluginDescriptors(
  private val firstDescriptorPath: String,
  private val firstDescriptorContainingFileName: String,
  private val secondDescriptorPath: String,
  private val secondDescriptorContainingFileName: String
) : PluginDescriptorResolutionError() {
  override val message: String
    get() {
      val firstIsLess = when {
        firstDescriptorPath < secondDescriptorPath -> true
        firstDescriptorPath == secondDescriptorPath -> firstDescriptorContainingFileName <= secondDescriptorContainingFileName
        else -> false
      }

      val (path1, file1) = if (firstIsLess) {
        firstDescriptorPath to firstDescriptorContainingFileName
      } else {
        secondDescriptorPath to secondDescriptorContainingFileName
      }

      val (path2, file2) = if (firstIsLess) {
        secondDescriptorPath to secondDescriptorContainingFileName
      } else {
        firstDescriptorPath to firstDescriptorContainingFileName
      }

      return "Found multiple plugin descriptors '$path1' from '$file1' and '$path2' from '$file2'."
    }
}
