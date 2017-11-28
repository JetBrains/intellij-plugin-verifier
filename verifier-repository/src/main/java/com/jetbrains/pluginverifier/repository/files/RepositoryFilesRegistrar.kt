package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.cleanup.fileSize
import java.nio.file.Path

/**
 * Data structures that maintains set of registered files and their total size.
 */
internal data class RepositoryFilesRegistrar<K>(var totalSpaceUsage: SpaceAmount = SpaceAmount.ZERO_SPACE,
                                                val files: MutableMap<K, FileInfo> = hashMapOf()) {
  fun addFile(key: K, file: Path) {
    assert(key !in files)
    val fileSize = file.fileSize
    FileRepositoryImpl.LOG.debug("Adding file by $key of size $fileSize: $file")
    totalSpaceUsage += fileSize
    files[key] = FileInfo(file, fileSize)
  }

  fun getAllKeys() = files.keys

  fun has(key: K) = key in files

  fun get(key: K) = files[key]

  fun deleteFile(key: K) {
    assert(key in files)
    val (file, size) = files[key]!!
    FileRepositoryImpl.LOG.debug("Deleting file by $key of size $size: $file")
    totalSpaceUsage -= size
    files.remove(key)
    file.deleteLogged()
  }
}