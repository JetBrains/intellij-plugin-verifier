/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

abstract class InvalidDescriptorProblem(private val descriptorPath: String?) : PluginProblem() {
  abstract val detailedMessage: String

  override val message
    get() = "Invalid plugin descriptor" + (if (descriptorPath.isNullOrEmpty()) ": " else " '$descriptorPath': ") + detailedMessage
}

class UnexpectedDescriptorElements(
  override val detailedMessage: String,
  descriptorPath: String? = null
) : InvalidDescriptorProblem(descriptorPath) {

  override val level
    get() = Level.ERROR

}

class PropertyNotSpecified(
  private val propertyName: String,
  descriptorPath: String? = null
) : InvalidDescriptorProblem(descriptorPath) {

  override val detailedMessage: String
    get() = "<$propertyName> is not specified"

  override val level
    get() = Level.ERROR
}

class NotBoolean(
  private val propertyName: String,
  descriptorPath: String? = null
) : InvalidDescriptorProblem(descriptorPath) {

  override val detailedMessage: String
    get() = "<$propertyName> must be boolean"

  override val level
    get() = Level.ERROR
}

class NotNumber(
  private val propertyName: String,
  descriptorPath: String? = null
) : InvalidDescriptorProblem(descriptorPath) {

  override val detailedMessage: String
    get() = "<$propertyName> must be integer"

  override val level
    get() = Level.ERROR
}

class UnableToReadDescriptor(descriptorPath: String, private val exceptionMessage: String?) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "Unable to read plugin descriptor" + (exceptionMessage?.let { ": $exceptionMessage" } ?: "")

  override val level
    get() = Level.ERROR
}

class ContainsNewlines(propertyName: String, descriptorPath: String? = null) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage = "<$propertyName> can't contain newlines"

  override val level = Level.ERROR
}