package org.jetbrains.plugins.verifier.service.tests

import org.jetbrains.plugins.verifier.service.tasks.ProgressIndicator
import org.jetbrains.plugins.verifier.service.tasks.ServiceTask
import org.jetbrains.plugins.verifier.service.tasks.ServiceTaskManager
import org.jetbrains.plugins.verifier.service.tasks.ServiceTaskStatus
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class TaskManagerTest {
  @Test
  fun `general functionality of the service task manager`() {
    ServiceTaskManager(4).use { tm ->
      val finish = AtomicBoolean()

      val serviceTask: ServiceTask<Int> = object : ServiceTask<Int>("testTask") {
        override fun execute(progress: ProgressIndicator): Int {
          while (!finish.get()) {
          }
          return 42
        }
      }

      val status = AtomicReference<ServiceTaskStatus>()
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