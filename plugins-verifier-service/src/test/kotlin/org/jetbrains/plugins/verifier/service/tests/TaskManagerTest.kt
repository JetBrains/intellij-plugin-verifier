package org.jetbrains.plugins.verifier.service.tests

import org.jetbrains.plugins.verifier.service.tasks.ProgressIndicator
import org.jetbrains.plugins.verifier.service.tasks.Task
import org.jetbrains.plugins.verifier.service.tasks.TaskDescriptor
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import org.junit.Assert.*
import org.junit.Test
import java.util.*
import java.util.concurrent.RunnableFuture
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
}