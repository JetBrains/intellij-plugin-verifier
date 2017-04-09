package com.jetbrains.pluginverifier.repository

import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.format.UpdateInfo

import java.io.File
import java.io.IOException

interface PluginRepository {

  @Throws(IOException::class)
  fun getLastCompatibleUpdates(ideVersion: IdeVersion): List<UpdateInfo>

  @Throws(IOException::class)
  fun getLastCompatibleUpdateOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo?

  @Throws(IOException::class)
  fun getAllCompatibleUpdatesOfPlugin(ideVersion: IdeVersion, pluginId: String): List<UpdateInfo>

  @Throws(IOException::class)
  fun getPluginFile(updateId: Int): FileLock?

  @Throws(IOException::class)
  fun getPluginFile(update: UpdateInfo): FileLock?

  @Throws(IOException::class)
  fun getUpdateInfoById(updateId: Int): UpdateInfo

}

interface FileLock {

  fun getFile(): File

  fun release()

}

data class IdleFileLock(val content: File) : FileLock {
  override fun getFile(): File = content

  override fun release() {
    //do nothing.
  }

}