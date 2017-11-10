package org.jetbrains.plugins.verifier.service.server.status

import org.apache.commons.io.FileUtils
import java.io.File

data class DiskUsageInfo(val totalUsage: Long)

fun File.getDiskUsage(): DiskUsageInfo {
  val size = FileUtils.sizeOf(this)
  return DiskUsageInfo(size)
}