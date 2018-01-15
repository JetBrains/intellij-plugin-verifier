package org.jetbrains.plugins.verifier.service.tasks

import com.jetbrains.pluginverifier.misc.formatDuration
import java.time.Duration
import java.time.Instant

/**
 * Descriptor of the [task] [ServiceTask] being executed.
 */
data class ServiceTaskStatus(
    /**
     * Unique ID of the task that can be
     * used to identify the task in the tasks queue.
     */
    val taskId: Long,

    /**
     * Presentable name of the task that
     * can be shown on the status page.
     */
    val presentableName: String,

    /**
     * Progress indicator that can be
     * used to monitor and control the execution
     * of the task.
     */
    val progress: ProgressIndicator,

    /**
     * Point in time when the task was started.
     */
    val startTime: Instant,

    /**
     * Point in time when the task was completed,
     * either successfully or abnormally.
     *
     * `null` if the task is not completed yet.
     */
    @Volatile
    var endTime: Instant?,

    /**
     * [State] [ServiceTaskState] of the task.
     */
    @Volatile
    var state: ServiceTaskState
) {

  /**
   * If the task is completed, returns the total execution time,
   * otherwise returns the amount of time since the task has started.
   */
  val elapsedTime: Duration
    get() = Duration.between(startTime, endTime ?: Instant.now())

  override fun toString(): String = buildString {
    append("(")
    append("Id=$taskId; ")
    append("State=$state; ")
    append("Time=${elapsedTime.formatDuration("S")} ms; ")
    append("Progress=" + progress.fraction + "; ")
    append("Text=" + progress.text + "; ")
    append("Task-name=" + presentableName)
    append(")")
  }

  override fun equals(other: Any?) = other is ServiceTaskStatus && taskId == other.taskId

  override fun hashCode() = taskId.hashCode()
}