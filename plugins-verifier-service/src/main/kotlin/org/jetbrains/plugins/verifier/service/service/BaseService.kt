package org.jetbrains.plugins.verifier.service.service

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.jetbrains.plugins.verifier.service.server.ServerInstance
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

abstract class BaseService(val serviceName: String,
                           private val initialDelay: Long,
                           private val period: Long,
                           private val timeUnit: TimeUnit) {

  enum class State {
    NOT_STARTED, SLEEPING, RUNNING, PAUSED, STOPPED
  }

  protected val LOG: Logger = LoggerFactory.getLogger(serviceName)

  private var state: State = State.NOT_STARTED

  private val executor by lazy {
    Executors.newSingleThreadScheduledExecutor(
        ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("$serviceName-%d")
            .build()
    )
  }

  protected val pluginRepositoryUserName: String = Settings.PLUGIN_REPOSITORY_VERIFIER_USERNAME.get()

  protected val pluginRepositoryPassword: String = Settings.PLUGIN_REPOSITORY_VERIFIER_PASSWORD.get()

  protected val taskManager = ServerInstance.taskManager

  fun getState() = state

  @Synchronized
  fun start(): Boolean {
    if (state == State.NOT_STARTED) {
      LOG.info("Starting $serviceName")
      state = State.SLEEPING
      executor.scheduleAtFixedRate({ onServeTime() }, initialDelay, period, timeUnit)
      return true
    }
    return false
  }

  @Synchronized
  fun pause(): Boolean {
    if (state == State.SLEEPING) {
      LOG.info("Pausing $serviceName")
      state = State.PAUSED
      return true
    }
    return false
  }

  @Synchronized
  fun resume(): Boolean {
    if (state == State.PAUSED) {
      LOG.info("Resuming $serviceName")
      state = State.SLEEPING
      return true
    }
    return false
  }

  @Synchronized
  fun stop(): Boolean {
    if (state == State.PAUSED || state == State.SLEEPING) {
      LOG.info("Stopping $serviceName")
      state = State.STOPPED
      executor.shutdownNow()
      return true
    }
    return false
  }

  @Synchronized
  private fun onServeTime() {
    if (state == State.SLEEPING) {
      state = State.RUNNING
      val start = System.currentTimeMillis()
      try {
        LOG.info("$serviceName is going to start")
        doServe()
      } catch (e: Throwable) {
        LOG.error("$serviceName failed to serve", e)
      } finally {
        val duration = System.currentTimeMillis() - start
        LOG.info("$serviceName has served in $duration ms")
        state = State.SLEEPING
      }
    }
  }

  protected abstract fun doServe()
}