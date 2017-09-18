package com.jetbrains.pluginverifier.core

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.misc.bytesToMegabytes
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.plugin.PluginCreator
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Patrikeev
 */
class VerifierExecutor(val params: VerifierParams) : Closeable {

  companion object {
    private val LOG = LoggerFactory.getLogger(VerifierExecutor::class.java)

    private val AVERAGE_VERIFIER_MEMORY = 200 * FileUtils.ONE_MB
  }

  private val runtimeResolver = Resolver.createJdkResolver(params.jdkDescriptor.homeDir)

  private val concurrentWorkers = estimateNumberOfConcurrentWorkers()

  private val executor = Executors.newFixedThreadPool(concurrentWorkers,
      ThreadFactoryBuilder()
          .setDaemon(true)
          .setNameFormat("verifier-%d")
          .build()
  )

  private val completionService = ExecutorCompletionService<Result>(executor)

  init {
    LOG.info("Created verifier with $concurrentWorkers " + " worker".pluralize(concurrentWorkers))
  }

  private fun estimateNumberOfConcurrentWorkers(): Int {
    val availableMemory = Runtime.getRuntime().maxMemory()
    val availableCpu = Runtime.getRuntime().availableProcessors().toLong()
    LOG.info("Available memory: ${availableMemory.bytesToMegabytes()} Mb; Available CPU = $availableCpu")
    val maxByMemory = availableMemory / AVERAGE_VERIFIER_MEMORY
    return maxOf(4, minOf(maxByMemory, availableCpu)).toInt()
  }

  fun verify(tasks: List<Pair<PluginCoordinate, IdeDescriptor>>, progress: Progress, pluginCreator: PluginCreator): List<Result> {
    tasks.forEach {
      val worker = Verifier(it.first, it.second, runtimeResolver, params, pluginCreator)
      completionService.submit(worker)
    }
    return getResults(tasks.size, progress)
  }

  override fun close() {
    runtimeResolver.closeLogged()
    executor.shutdownNow()
  }

  private fun getResults(tasks: Int, progress: Progress): List<Result> {
    var verified = 0
    val results = arrayListOf<Result>()
    (1..tasks).forEach fori@ {
      while (true) {
        if (Thread.currentThread().isInterrupted) {
          throw InterruptedException()
        }

        val future = completionService.poll(500, TimeUnit.MILLISECONDS)
        if (future != null) {
          val result = future.get()
          results.add(result)
          progress.setProgress(((++verified).toDouble()) / tasks)
          val resultString = result.toString()
          progress.setText(resultString)
          LOG.info("$verified/$tasks plugins finished. $resultString")
          break
        }
      }
    }
    return results
  }

}