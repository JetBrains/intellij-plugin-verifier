package org.jetbrains.plugins.verifier.service.tasks

/**
 * @author Sergey Patrikeev
 */
abstract class ServiceTask<out R> {

  abstract fun computeResult(progress: ServiceTaskProgress): R

  abstract fun presentableName(): String

  override fun toString(): String = presentableName()

}