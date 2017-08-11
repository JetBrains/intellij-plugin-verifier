package org.jetbrains.plugins.verifier.service.service

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.gson.Gson
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

abstract class BaseService(val serviceName: String,
                           initialDelay: Long,
                           period: Long,
                           timeUnit: TimeUnit) {

  enum class State {
    SLEEPING, RUNNING, PAUSED, STOPPED
  }

  companion object {
    val GSON: Gson = Gson()
  }

  protected val LOG: Logger = LoggerFactory.getLogger(serviceName)

  private var state: State = State.SLEEPING

  private val executor = Executors.newSingleThreadScheduledExecutor(
      ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat("$serviceName-%d")
          .build()
  )

  init {
    executor.scheduleAtFixedRate({ tick() }, initialDelay, period, timeUnit)
  }

  protected val pluginRepositoryUserName: String = Settings.PLUGIN_REPOSITORY_VERIFIER_USERNAME.get()

  protected val pluginRepositoryPassword: String = Settings.PLUGIN_REPOSITORY_VERIFIER_PASSWORD.get()

  protected val taskManager = ServerInstance.taskManager

  fun getState() = state

  @Synchronized
  fun pause() {
    state = State.PAUSED
  }

  @Synchronized
  fun resume() {
    state = State.SLEEPING
  }

  @Synchronized
  fun stop() {
    if (state != State.STOPPED) {
      state = State.STOPPED
      LOG.info("Stopping $serviceName")
      executor.shutdownNow()
    }
  }

  @Synchronized
  protected fun tick() {
    if (state == State.SLEEPING) {
      state = State.RUNNING
      val start = System.currentTimeMillis()
      try {
        LOG.info("$serviceName is going to start")
        doTick()
      } catch (e: Throwable) {
        LOG.error("$serviceName failed to serve", e)
      } finally {
        val duration = System.currentTimeMillis() - start
        LOG.info("$serviceName has served in $duration ms")
        state = State.SLEEPING
      }
    }
  }

  protected abstract fun doTick()
}