package org.jetbrains.plugins.verifier.service.tasks

import com.google.common.collect.EvictingQueue
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.jetbrains.pluginverifier.misc.findCause
import com.jetbrains.pluginverifier.misc.shutdownAndAwaitTermination
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.time.Instant
import java.util.*
import java.util.concurrent.*

/**
 * Enqueues tasks and executes completion callbacks.
 */
class TaskManager(concurrency: Int) : Closeable {
  private companion object {
    private val LOG = LoggerFactory.getLogger(TaskManager::class.java)
  }

  private var nextTaskId: Long = 0

  private val executorService = object : ThreadPoolExecutor(
      concurrency,
      concurrency,
      0L,
      TimeUnit.MILLISECONDS,
      PriorityBlockingQueue(),
      ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat("worker-%d")
          .build()
  ) {

    override fun <T : Any?> newTaskFor(runnable: Runnable?, value: T): RunnableFuture<T> {
      if (runnable is PriorityTask<*>) {
        @Suppress("UNCHECKED_CAST")
        return runnable as PriorityTask<T>
      }
      return super.newTaskFor(runnable, value)
    }

  }

  private val _activeTasks = Collections.synchronizedSet(linkedSetOf<TaskDescriptor>())

  private val _finishedTasks = Collections.synchronizedCollection(EvictingQueue.create<TaskDescriptor>(100))

  /**
   * Tasks with state either [TaskDescriptor.State.WAITING] or [TaskDescriptor.State.RUNNING]
   */
  val activeTasks: Set<TaskDescriptor>
    @Synchronized
    get() = _activeTasks.toSet()

  /**
   * Tasks with state either [TaskDescriptor.State.SUCCESS], [TaskDescriptor.State.ERROR] or [TaskDescriptor.State.CANCELLED]
   */
  val finishedTasks: Set<TaskDescriptor>
    @Synchronized
    get() = _finishedTasks.toSet()

  /**
   * Enqueues the [task] to be executed on a background
   * thread and returns a [descriptor] [TaskDescriptor] of the task
   * that can be used to monitor the task's state.
   *
   * The [onSuccess] callback with the task's result and
   * descriptor will be called on a background thread
   * if the task is finished successfully
   *
   * The [onError] callback with the thrown exception and
   * descriptor will be called on a background thread
   * if the task is finished abnormally.
   *
   * The [onCompletion] callback will be invoked when then task
   * completes, either successfully or abnormally.
   */
  @Synchronized
  fun <T> enqueue(task: Task<T>,
                  onSuccess: (T, TaskDescriptor) -> Unit,
                  onError: (Throwable, TaskDescriptor) -> Unit,
                  onCancelled: (Throwable, TaskDescriptor) -> Unit,
                  onCompletion: (TaskDescriptor) -> Unit): TaskDescriptor {
    val taskId = ++nextTaskId

    val taskProgress = DefaultProgressIndicator()
    taskProgress.fraction = 0.0
    taskProgress.text = "Waiting to start..."

    val descriptor = TaskDescriptor(
        taskId,
        task.presentableName,
        taskProgress,
        Instant.now(),
        null,
        TaskDescriptor.State.WAITING
    )
    _activeTasks.add(descriptor)

    val runnable = createRunner(task, descriptor, onSuccess, onError, onCancelled, onCompletion)
    val futureTask = FutureTask<T>(runnable, null)
    val priorityTask = PriorityTask(taskId, task, futureTask)
    executorService.submit(priorityTask)

    return descriptor
  }

  private fun <T> createRunner(
      task: Task<T>,
      descriptor: TaskDescriptor,
      onSuccess: (T, TaskDescriptor) -> Unit,
      onError: (Throwable, TaskDescriptor) -> Unit,
      onCancelled: (Throwable, TaskDescriptor) -> Unit,
      onCompletion: (TaskDescriptor) -> Unit
  ) = Runnable {
    with(descriptor) {
      state = TaskDescriptor.State.RUNNING
      progress.text = "Running..."
      try {
        try {
          val result = try {
            task.execute(progress)
          } finally {
            endTime = Instant.now()
            progress.fraction = 1.0
          }
          state = TaskDescriptor.State.SUCCESS
          progress.text = "Finished successfully"
          onSuccess(result, descriptor)
        } catch (e: Throwable) {
          val ie = e.findCause(InterruptedException::class.java)
          if (ie != null) {
            state = TaskDescriptor.State.CANCELLED
            progress.text = ie.message ?: "Cancelled"
            onCancelled(ie, descriptor)
          } else {
            state = TaskDescriptor.State.ERROR
            progress.text = "Finished with error"
            onError(e, descriptor)
          }
        }
      } finally {
        _activeTasks.remove(descriptor)
        _finishedTasks.add(descriptor)
        onCompletion(descriptor)
      }
    }
  }

  /**
   * Enqueues the task without registering the
   * completion callbacks.
   * To specify the callbacks use [enqueue] method instead.
   */
  @Synchronized
  fun <T> enqueue(task: Task<T>) = enqueue(
      task,
      { _, _ -> },
      { _, _ -> },
      { _, _ -> },
      { _ -> }
  )

  @Synchronized
  override fun close() {
    LOG.info("Stopping task manager")
    executorService.shutdownAndAwaitTermination(1, TimeUnit.MINUTES)
  }

}