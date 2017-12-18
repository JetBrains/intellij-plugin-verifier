package org.jetbrains.plugins.verifier.service.tasks

/**
 * This enum class represents the state of a
 * [task] [ServiceTask] being executed.
 *
 * Initially, the task is in the [waiting] [WAITING] queue.
 * Then the task is [being executed] [RUNNING] and finally
 * its result is either [success] [SUCCESS] or [failure] [ERROR].
 */
enum class ServiceTaskState {
  WAITING,
  RUNNING,
  SUCCESS,
  ERROR,
}