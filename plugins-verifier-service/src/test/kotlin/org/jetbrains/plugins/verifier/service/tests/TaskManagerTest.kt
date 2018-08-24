package org.jetbrains.plugins.verifier.service.tests

import org.jetbrains.plugins.verifier.service.tasks.ProgressIndicator
import org.jetbrains.plugins.verifier.service.tasks.Task
import org.jetbrains.plugins.verifier.service.tasks.TaskDescriptor
import org.jetbrains.plugins.verifier.service.tasks.TaskManagerImpl
import org.junit.Assert.*
import org.junit.Test
import java.util.*
import java.util.Collections.synchronizedList
import java.util.Collections.synchronizedSet
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class TaskManagerTest {
  @Test
  fun `general functionality of the task manager`() {
    TaskManagerImpl(4).use { tm ->
      val finish = AtomicBoolean()

      val serviceTask: Task<Int> = object : Task<Int>("testTask", "testTask") {
        override fun execute(progress: ProgressIndicator): Int {
          while (!finish.get()) {
          }
          return 42
        }
      }

      val status = AtomicReference<TaskDescriptor>()
      val success = AtomicReference<Int>()
      val error = AtomicReference<Throwable>()

      tm.enqueue(
          serviceTask,
          onSuccess = { result, _ ->
            success.set(result)
          },
          onError = { e, _ ->
            error.set(e)
          },
          onCompletion = {
            status.set(it)
          }
      )

      assertEquals(1, tm.activeTasks.size)
      val taskInfo = tm.activeTasks.values.first().first()

      assertEquals(0.0, taskInfo.progress.fraction, 1e-5)
      finish.set(true)

      val start = System.currentTimeMillis()
      while (status.get() == null) {
        assertTrue((System.currentTimeMillis() - start) < 5000)
      }

      assertEquals(1.0, taskInfo.progress.fraction, 1e-5)
      assertNull(error.get())
    }
  }

  /**
   * Schedules tasks with random priorities
   * and verifies that the tasks with higher
   * priorities were run earlier.
   */
  @Test
  fun `tasks are run by decrease of priorities`() {
    //Flag indicating test start.
    val start = CountDownLatch(1)

    val totalTasks = 128

    /**
     * First task will be scheduled first.
     */
    val random = Random()
    val priorities = listOf(Integer.MAX_VALUE) + (1 until totalTasks).map { random.nextInt() }

    val notRun = synchronizedList(priorities.toMutableList())

    val error = AtomicReference<String>()

    class TestTask(val priority: Int) : Task<Int>("test", "test"), Comparable<TestTask> {
      override fun execute(progress: ProgressIndicator): Int {
        //Wait for the test to start.
        start.await()
        synchronized(notRun) {
          notRun.remove(priority)
          if (notRun.any { it > priority }) {
            error.set("Not highest priority task was started")
          }
        }
        return 0
      }

      override fun compareTo(other: TestTask) = Integer.compare(other.priority, priority)
    }

    TaskManagerImpl(1).use { tm ->
      priorities
          .map { TestTask(it) }
          .forEach { tm.enqueue(it) }
      start.countDown()
    }

    val msg = error.get()
    if (msg != null) {
      fail(msg)
    }
  }

  /**
   * Creates Task Manager for 4 threads.
   *
   * Schedules 8 tasks:
   * - 4 tasks are waiting for interruption
   * - 4 tasks will not be polled from the waiting queue
   *
   * Check that 'onCompletion' is not called for any cancelled task.
   */
  @Test
  fun `onCompletion callback must not be called for cancelled tasks`() {
    val runTasks = synchronizedList(arrayListOf<Int>())
    val completedTasks = synchronizedList(arrayListOf<Int>())

    class TestTask(val index: Int) : Task<Int>("test", "test"), Comparable<TestTask> {
      override fun execute(progress: ProgressIndicator): Int {
        runTasks.add(index)
        while (!Thread.currentThread().isInterrupted) {
        }
        throw InterruptedException()
      }

      //Run tasks according to their priorities (descending).
      override fun compareTo(other: TestTask) = Integer.compare(other.index, index)
    }

    TaskManagerImpl(4).use { taskManager ->
      val descriptors = (0 until 8).map { index ->

        taskManager.enqueue(
            TestTask(index),
            { _, _ -> },
            { _, _ -> },
            { _ -> completedTasks.add(index) }
        )
      }

      /**
       * Wait until all threads of this Task Manager are started.
       */
      while (runTasks.size < 4) {
      }

      /**
       * Check that the most prioritized tasks are started.
       */
      assertEquals(listOf(0, 1, 2, 3), runTasks.sorted())

      /**
       * Cancel all the tasks
       */
      for (descriptor in descriptors) {
        taskManager.cancel(descriptor)
      }

      assertEquals(emptyList<Int>(), completedTasks)
    }
  }

  /**
   * Tests that the [TaskManager] executes tasks of different types
   * in separate executors. It guarantees that no task will starve
   * because some different tasks have greater priority.
   */
  @Test
  fun `tasks of different types are executed in parallel`() {
    class TaskFast : Task<Int>("task1", "task1") {
      override fun execute(progress: ProgressIndicator) = 42
    }

    val start = AtomicBoolean()

    class TaskLong : Task<Int>("task2", "task2") {
      override fun execute(progress: ProgressIndicator): Int {
        while (!start.get()) {
        }
        return 42
      }
    }

    TaskManagerImpl(4).use { taskManager ->
      /**
       * Submit 16 tasks waiting for start.
       */
      for (i in 0 until 16) {
        taskManager.enqueue(TaskLong())
      }

      val finishedTasks = synchronizedSet(hashSetOf<Long>())
      val finishedTaskIds = (0 until 4)
          .map {
            taskManager.enqueue(
                TaskFast(),
                onCompletion = { td -> finishedTasks.add(td.taskId) }
            )
          }.mapTo(hashSetOf()) { it.taskId }

      val startTime = System.currentTimeMillis()
      while (finishedTasks.size != 4 && System.currentTimeMillis() - startTime < 5000) {
      }

      try {
        assertEquals(finishedTaskIds, finishedTasks)
      } finally {
        /**
         * Start all long tasks and allow the task manager to exit.
         */

        //finally block is necessary here to guarantee that this
        // line is executed in case the above assertion throws an exception.
        start.set(true)
      }
    }
  }
}