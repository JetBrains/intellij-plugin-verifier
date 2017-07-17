package org.jetbrains.plugins.verifier.service.tasks

/**
 * @author Sergey Patrikeev
 */
abstract class Task<out R> {

  abstract fun computeResult(progress: TaskProgress): R

  abstract fun presentableName(): String

  override fun toString(): String = presentableName()

}