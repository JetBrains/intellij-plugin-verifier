package org.jetbrains.plugins.verifier.service.tasks

/**
 * @author Sergey Patrikeev
 */
data class ServiceTaskId(val id: Long) {
  override fun toString(): String = id.toString()
}