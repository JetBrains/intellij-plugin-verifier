package org.jetbrains.plugins.verifier.service.tasks

/**
 * @author Sergey Patrikeev
 */
data class TaskId(val id: Int) {
  override fun toString(): String = id.toString()
}