package org.jetbrains.plugins.verifier.service.storage

import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.misc.extractTo
import com.jetbrains.pluginverifier.misc.replaceInvalidFileNameCharacters
import org.slf4j.LoggerFactory
import java.io.File

//todo: provide a cache of IdeDescriptors
//todo: merge it with IdeRepository from verifier module
class IdeFilesManager(private val fileManager: FileManager,
                      private val ideFilesDir: File) {

  private val LOG = LoggerFactory.getLogger(IdeFilesManager::class.java)

  private val ideCache: MutableMap<IdeVersion, File> = hashMapOf()
  private val lockedIdes: MutableMap<IdeVersion, Int> = hashMapOf()
  private val deleteQueue: MutableSet<IdeVersion> = hashSetOf()

  private inner class IdeFileLockImpl(override val ideFile: File, override val ideVersion: IdeVersion) : IdeFileLock {
    override fun close() = releaseLock(this)

    override fun toString(): String = ideVersion.toString()
  }

  @Synchronized
  private fun releaseLock(lock: IdeFileLockImpl) {
    val version = IdeVersion.createIdeVersion(lock.ideFile.name)
    var cnt = lockedIdes[version] ?: return
    cnt--
    if (cnt == 0) {
      lockedIdes.remove(version)
      ideCache.remove(version)
      onRelease(version)
    } else {
      lockedIdes.put(version, cnt)
    }
  }

  @Synchronized
  fun <R> lockAndAccess(block: () -> R): R = block()

  private fun onRelease(version: IdeVersion) {
    if (deleteQueue.contains(version)) {
      deleteQueue.remove(version)
      val ideFile = getIdeFileForVersion(version)
      LOG.info("Deleting the IDE file $ideFile")
      if (ideFile.isDirectory) {
        ideFile.deleteLogged()
      }
    }
  }

  private fun getIdeFileForVersion(version: IdeVersion) =
      ideFilesDir.resolve(version.asString().replaceInvalidFileNameCharacters())

  @Synchronized
  fun ideList(): List<IdeVersion> = ideFilesDir
      .listFiles()
      .mapNotNull { createIdeVersionSafe(it) }
      .toList()

  private fun createIdeVersionSafe(ideFile: File) = try {
    IdeVersion.createIdeVersion(ideFile.name)
  } catch (e: Exception) {
    null
  }

  @Synchronized
  fun getIdeLock(version: IdeVersion): IdeFileLock? {
    val ideFile = getIdeFileForVersion(version)
    if (!ideFile.isDirectory) {
      return null
    }

    val ide = ideCache.getOrPut(version, { ideFile })
    val cnt = lockedIdes.getOrPut(version, { 0 })
    lockedIdes.put(version, cnt + 1)
    return IdeFileLockImpl(ide, version)
  }

  @Synchronized
  fun deleteIde(version: IdeVersion) {
    LOG.info("Deleting IDE #$version")
    deleteQueue.add(version)
    if (!lockedIdes.contains(version)) {
      onRelease(version)
    }
  }

  @Synchronized
  fun addIde(ideFile: File): Boolean {
    LOG.info("Adding IDE from file $ideFile")
    if (!ideFile.exists()) {
      throw IllegalArgumentException("The IDE file $ideFile doesn't exist")
    }

    if (ideFile.isDirectory) {
      val version: IdeVersion = try {
        IdeManager.createManager().createIde(ideFile).version
      } catch (e: Exception) {
        LOG.error("The IDE file $ideFile is invalid", e)
        throw e
      }
      return addIde(ideFile, version)
    }

    if (ideFile.isFile) {
      val tempDirectory = fileManager.createTempDirectory(ideFile.name)

      try {
        try {
          ideFile.extractTo(tempDirectory)
        } catch (e: Exception) {
          LOG.error("Unable to extract $ideFile", e)
          throw e
        }

        val version: IdeVersion = try {
          IdeManager.createManager().createIde(tempDirectory).version
        } catch (e: Exception) {
          LOG.error("The IDE file $tempDirectory is not a valid IDE", e)
          throw e
        }
        return addIde(tempDirectory, version)
      } finally {
        tempDirectory.deleteLogged()
      }
    }

    throw IllegalArgumentException("Invalid file $ideFile")
  }

  private fun addIde(ideDir: File, version: IdeVersion): Boolean {
    if (lockedIdes.contains(version)) {
      return false
    }

    val destination = getIdeFileForVersion(version)
    try {
      ideDir.copyRecursively(destination, true)
      LOG.info("IDE #$version is saved")
    } catch (e: Exception) {
      destination.deleteLogged()
      throw e
    }

    return true
  }

}