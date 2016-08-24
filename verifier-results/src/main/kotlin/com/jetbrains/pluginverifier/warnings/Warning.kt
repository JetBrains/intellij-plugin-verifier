package com.jetbrains.pluginverifier.warnings

import com.google.gson.annotations.SerializedName

/**
 * @author Sergey Patrikeev
 */
data class Warning(@SerializedName("msg") val message: String) {
  override fun toString(): String = message
}