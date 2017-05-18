package com.jetbrains.pluginverifier.repository

import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.format.UpdateInfo
import java.io.File

interface PluginRepository {

  fun getLastCompatibleUpdates(ideVersion: IdeVersion): List<UpdateInfo>

  fun getLastCompatibleUpdateOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo?

  fun getAllCompatibleUpdatesOfPlugin(ideVersion: IdeVersion, pluginId: String): List<UpdateInfo>

  fun getPluginFile(updateId: Int): FileLock?

  fun getPluginFile(update: UpdateInfo): FileLock?

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