package org.jetbrains.plugins.verifier.service.service

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.gson.Gson
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

abstract class BaseService(private val serviceName: String,
                           private val initialDelay: Long,
                           private val period: Long,
                           private val timeUnit: TimeUnit,
                           protected val taskManager: TaskManager) {

  protected val LOG: Logger = LoggerFactory.getLogger(serviceName)

  protected val GSON: Gson = Gson()

  private var isServing: Boolean = false

  private val executor = Executors.newSingleThreadScheduledExecutor(
      ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat("$serviceName-%d")
          .build()
  )

  protected val pluginRepositoryUserName: String = Settings.PLUGIN_REPOSITORY_VERIFIER_USERNAME.get()

  protected val pluginRepositoryPassword: String = Settings.PLUGIN_REPOSITORY_VERIFIER_PASSWORD.get()

  fun start() {
    executor.scheduleAtFixedRate({ tick() }, initialDelay, period, timeUnit)
  }

  fun stop() {
    LOG.info("Stopping $serviceName")
    executor.shutdownNow()
  }

  @Synchronized
  protected fun tick() {
    if (isServing) {
      LOG.info("$serviceName is already in progress")
      return
    }

    isServing = true
    val start = System.currentTimeMillis()
    try {
      if (taskManager.isBusy()) {
        LOG.info("Task manager is full now")
        return
      }
      LOG.info("$serviceName is going to start")
      doTick()
    } catch (e: Throwable) {
      LOG.error("$serviceName failed to serve", e)
    } finally {
      val duration = System.currentTimeMillis() - start
      LOG.info("$serviceName has served in $duration ms")
      isServing = false
    }
  }

  protected abstract fun doTick()
}