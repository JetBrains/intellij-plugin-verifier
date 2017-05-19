package com.jetbrains.pluginverifier.api

import com.jetbrains.pluginverifier.format.UpdateInfo
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.repository.FileLock
import java.io.Closeable

sealed class PluginDescriptor : Closeable {

  abstract val presentableName: String

  data class ByUpdateInfo(val updateInfo: UpdateInfo) : PluginDescriptor() {
    override val presentableName: String = updateInfo.toString()

    override fun close() = Unit

    override fun toString(): String = presentableName
  }

  data class ByFileLock(val fileLock: FileLock) : PluginDescriptor() {
    override val presentableName: String = fileLock.getFile().toString()

    override fun close() = fileLock.release()

    override fun toString(): String = presentableName
  }

  data class ByInstance(val createOk: CreatePluginResult.OK) : PluginDescriptor() {
    override val presentableName: String = createOk.success.plugin.toString()

    override fun close() = createOk.close()

    override fun toString(): String = presentableName
  }
}