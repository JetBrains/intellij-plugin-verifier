/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.problems

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import java.net.URL

abstract class InvalidDescriptorProblem(private val descriptorPath: String?) : PluginProblem() {
  abstract val detailedMessage: String

  override val message
    get() = "Invalid plugin descriptor" + (if (descriptorPath.isNullOrEmpty()) ": " else " '$descriptorPath': ") + detailedMessage
}


class InvalidPluginIDProblem(private val id: String) : PluginProblem() {
  override val message
    get() = "Plugin id contains unsupported symbols: $id."
  override val level
    get() = Level.ERROR
}

class UnexpectedDescriptorElements(
  override val detailedMessage: String,
  descriptorPath: String? = null
) : InvalidDescriptorProblem(descriptorPath) {

  override val level
    get() = Level.ERROR

}

class TooLongPropertyValue(
  descriptorPath: String,
  private val propertyName: String,
  private val propertyValueLength: Int,
  private val maxLength: Int
) : InvalidDescriptorProblem(descriptorPath) {
  override val detailedMessage: String
    get() = "value of property '$propertyName' is too long. Its length is $propertyValueLength which is more than maximum $maxLength characters long"

  override val level
    get() = Level.ERROR
}

open class PropertyNotSpecified(
  protected val propertyName: String,
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

class VendorCannotBeEmpty(descriptorPath: String? = null
) : PropertyNotSpecified("vendor", descriptorPath) {

  private val solutionHint = ProblemSolutionHint(
          """<vendor email="joe@example.com">Joe Doe</vendor>""",
          URL("https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html#idea-plugin__vendor")
  )

  override val detailedMessage: String
    get() = "<$propertyName> element has no content. The vendor name or organization ID must be set. Example: ${solutionHint.example}"

  override val level
    get() = Level.ERROR
}

