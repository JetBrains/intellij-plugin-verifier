package com.jetbrains.pluginverifier.descriptions

import java.text.MessageFormat

/**
 * @author Sergey Patrikeev
 */
data class ShortDescription(private val template: String, private val params: List<Any>) {
  override fun toString(): String {
    return MessageFormat.format(template, *params.toTypedArray())
  }
}