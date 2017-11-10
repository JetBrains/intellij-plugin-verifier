package org.jetbrains.plugins.verifier.service.server.status

data class MemoryInfo(val totalMemory: Long,
                      val freeMemory: Long,
                      val usedMemory: Long,
                      val maxMemory: Long)

fun getMemoryInfo(): MemoryInfo {
  val runtime = Runtime.getRuntime()
  return MemoryInfo(
      runtime.totalMemory(),
      runtime.freeMemory(),
      (runtime.totalMemory() - runtime.freeMemory()),
      runtime.maxMemory()
  )
}