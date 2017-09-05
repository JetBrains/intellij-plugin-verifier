package org.jetbrains.plugins.verifier.service.service

import com.google.gson.Gson
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import java.io.Closeable

/**
 * @author Sergey Patrikeev
 */
object ServerInstance : Closeable {
  val GSON: Gson = Gson()

  val taskManager = TaskManager(Settings.TASK_MANAGER_CONCURRENCY.getAsInt())

  val services = arrayListOf<BaseService>()

  fun addService(service: BaseService) {
    services.add(service)
  }

  override fun close() {
    taskManager.stop()
    services.forEach { it.stop() }
  }

}