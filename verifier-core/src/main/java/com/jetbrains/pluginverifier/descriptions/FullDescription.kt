package com.jetbrains.pluginverifier.descriptions

import java.text.MessageFormat

/**
 * @author Sergey Patrikeev
 */
data class FullDescription(private val template: String,
                           private val effect: String,
                           private val params: List<String>) {
  override fun toString(): String {
    val fullMessage = MessageFormat.format(template, *params.toTypedArray())
    return fullMessage + ". " + effect
  }
}