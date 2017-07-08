package org.jetbrains.plugins.verifier.service.ide

import com.intellij.structure.ide.IdeManager
import com.intellij.structure.ide.IdeVersion
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.misc.extractTo
import org.jetbrains.plugins.verifier.service.storage.FileManager
import org.jetbrains.plugins.verifier.service.storage.FileType
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.File

//TODO: improve IDE cache on high concurrency: don't recreate IDE instance after each lock release.
object IdeFilesManager {

  private val LOG = LoggerFactory.getLogger(IdeFilesManager::class.java)

  private val ideCache: MutableMap<IdeVersion, File> = hashMapOf()
  private val lockedIdes: MutableMap<IdeVersion, Int> = hashMapOf()
  private val deleteQueue: MutableSet<IdeVersion> = hashSetOf()

  private data class IdeFileLockImpl(private val ide: File) : IdeFileLock(), Closeable {

    override fun getIdeFile(): File = ide

    override fun release() = releaseLock(this)

  }

  @Synchronized
  private fun releaseLock(lock: IdeFileLockImpl) {
    val version = IdeVersion.createIdeVersion(lock.getIdeFile().name)
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
      val ideFile = FileManager.getFileByName(version.asString(), FileType.IDE)
      LOG.info("Deleting the IDE file $ideFile")
      if (ideFile.isDirectory) {
        ideFile.deleteLogged()
      }
    }
  }

  @Synchronized
  fun ideList(): List<IdeVersion> = FileManager.getFilesOfType(FileType.IDE).map { it -> IdeVersion.createIdeVersion(it.name) }.toList()

  @Synchronized
  fun getIdeLock(version: IdeVersion): IdeFileLock? {
    val ideFile = FileManager.getFileByName(version.asString(), FileType.IDE)
    if (!ideFile.isDirectory) {
      return null
    }

    val ide = ideCache.getOrPut(version, { ideFile })
    val cnt = lockedIdes.getOrPut(version, { 0 })
    lockedIdes.put(version, cnt + 1)
    return IdeFileLockImpl(ide)
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
        IdeManager.getInstance().createIde(ideFile).version
      } catch(e: Exception) {
        LOG.error("The IDE file $ideFile is invalid", e)
        throw e
      }
      return addIde(ideFile, version)
    }

    if (ideFile.isFile) {
      val tempDirectory = FileManager.createTempDirectory(ideFile.name)

      try {
        try {
          ideFile.extractTo(tempDirectory)
        } catch (e: Exception) {
          LOG.error("Unable to extract $ideFile", e)
          throw e
        }

        val version: IdeVersion = try {
          IdeManager.getInstance().createIde(tempDirectory).version
        } catch(e: Exception) {
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

    val destination = FileManager.getFileByName(version.asString(), FileType.IDE)
    try {
      ideDir.copyRecursively(destination, true)
      LOG.info("IDE #$version is saved")
    } catch(e: Exception) {
      destination.deleteLogged()
      throw e
    }

    return true
  }

}