package com.jetbrains.pluginverifier.tasks.profiling

import com.jetbrains.pluginverifier.tasks.TaskResult
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.measureTimeMillis

fun measurePluginVerification(measuredBlock: (Unit) -> TaskResult): MeasuredResult {
  val result = AtomicReference<TaskResult>()
  val durationInMillis: Long = measureTimeMillis {
    result.set(measuredBlock.invoke(Unit))
  }
  return MeasuredResult(result.get(), Duration.ofMillis(durationInMillis))
}

data class MeasuredResult(val taskResult: TaskResult, val duration: Duration)
