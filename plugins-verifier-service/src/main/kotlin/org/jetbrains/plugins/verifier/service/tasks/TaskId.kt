package org.jetbrains.plugins.verifier.service.tasks

import com.google.gson.annotations.SerializedName

/**
 * @author Sergey Patrikeev
 */
data class TaskId(@SerializedName("id") val id: Int) {
  override fun toString(): String = id.toString()
}