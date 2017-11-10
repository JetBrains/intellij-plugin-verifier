package org.jetbrains.plugins.verifier.service.service.tasks

import org.jetbrains.plugins.verifier.service.server.ServerContext

/**
 * @author Sergey Patrikeev
 */
abstract class ServiceTask(protected val serverContext: ServerContext) {

  abstract fun computeResult(progress: ServiceTaskProgress): ServiceTaskResult

  abstract fun presentableName(): String

  override fun toString(): String = presentableName()

}