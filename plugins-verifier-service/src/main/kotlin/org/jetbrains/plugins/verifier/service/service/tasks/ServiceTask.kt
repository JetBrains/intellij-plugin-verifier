package org.jetbrains.plugins.verifier.service.service.tasks

/**
 * @author Sergey Patrikeev
 */
abstract class ServiceTask {

  abstract fun computeResult(progress: ServiceTaskProgress): ServiceTaskResult

  abstract fun presentableName(): String

  override fun toString(): String = presentableName()

}