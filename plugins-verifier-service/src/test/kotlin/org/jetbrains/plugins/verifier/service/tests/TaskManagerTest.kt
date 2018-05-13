package org.jetbrains.plugins.verifier.service.tests

import org.jetbrains.plugins.verifier.service.tasks.ProgressIndicator
import org.jetbrains.plugins.verifier.service.tasks.Task
import org.jetbrains.plugins.verifier.service.tasks.TaskDescriptor
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import org.junit.Assert.*
import org.junit.Test
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class TaskManagerTest {
  @Test
  fun `general functionality of the service task manager`() {
    TaskManager(4).use { tm ->
      val finish = AtomicBoolean()

      val serviceTask: Task<Int> = object : Task<Int>("testTask") {
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
          onCancelled = { _, _ -> },
          onCompletion = {
            status.set(it)
          }
      )

      assertEquals(1, tm.activeTasks.size)
      val taskInfo = tm.activeTasks.first()

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

    val notRun = Collections.synchronizedList(priorities.toMutableList())

    val error = AtomicReference<String>()

    class TestTask(val priority: Int) : Task<Int>("test"), Comparable<TestTask> {
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

    TaskManager(1).use { tm ->
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
}