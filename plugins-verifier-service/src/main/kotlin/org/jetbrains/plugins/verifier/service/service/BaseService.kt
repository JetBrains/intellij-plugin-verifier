package org.jetbrains.plugins.verifier.service.service

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.jetbrains.pluginverifier.misc.shutdownAndAwaitTermination
import org.jetbrains.plugins.verifier.service.service.BaseService.State
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Service is a job that runs periodically with
 * initial delay of [initialDelay] and period of [period] measured in [timeUnit]s.
 *
 * Service has a state which is one of [State] constants.
 * Initially, the service is in [State.NOT_STARTED] state and can be started
 * by [start] invocation. The service can be [_paused_] [pause] and [_resumed_] [resume] several times,
 * but it can be [_started_] [start] and [_stopped_] [stop] only once.
 *
 * Service implementations may want to de-allocate resources when the service is stopped. This can be done
 * by overriding the [onStop].
 */
abstract class BaseService(val serviceName: String,
                           private val initialDelay: Long,
                           private val period: Long,
                           private val timeUnit: TimeUnit,
                           protected val taskManager: TaskManager) {

  enum class State {
    NOT_STARTED, SLEEPING, RUNNING, PAUSED, STOPPED
  }

  protected val logger: Logger = LoggerFactory.getLogger(serviceName)

  private var state: State = State.NOT_STARTED

  private val executor by lazy {
    Executors.newSingleThreadScheduledExecutor(
        ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("$serviceName-%d")
            .build()
    )
  }

  fun getState() = state

  @Synchronized
  fun start(): Boolean {
    if (state == State.NOT_STARTED) {
      logger.info("Starting $serviceName")
      state = State.SLEEPING
      executor.scheduleAtFixedRate({ onServeTime() }, initialDelay, period, timeUnit)
      return true
    }
    return false
  }

  @Synchronized
  fun pause(): Boolean {
    if (state == State.SLEEPING) {
      logger.info("Pausing $serviceName")
      state = State.PAUSED
      return true
    }
    return false
  }

  @Synchronized
  fun resume(): Boolean {
    if (state == State.PAUSED) {
      logger.info("Resuming $serviceName")
      state = State.SLEEPING
      return true
    }
    return false
  }

  @Synchronized
  fun stop(): Boolean {
    if (state == State.PAUSED || state == State.SLEEPING) {
      logger.info("Stopping $serviceName")
      state = State.STOPPED
      executor.shutdownAndAwaitTermination(1, TimeUnit.MINUTES)
      onStop()
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
        logger.info("$serviceName is going to start")
        doServe()
      } catch (ie: InterruptedException) {
        logger.info("$serviceName has been interrupted")
        Thread.currentThread().interrupt()
      } catch (e: Exception) {
        logger.error("$serviceName failed to serve", e)
      } finally {
        val duration = System.currentTimeMillis() - start
        logger.info("$serviceName has completed in $duration ms")
        state = State.SLEEPING
      }
    }
  }

  /**
   * This method is invoked when the next work cycle runs.
   * It is invoked under _synchronized_ block so the service implementations
   * are free to modify any state variables.
   */
  protected abstract fun doServe()

  /**
   * This method is called once when the service is stopped.
   */
  protected open fun onStop() = Unit
}