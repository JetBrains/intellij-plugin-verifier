package org.jetbrains.plugins.verifier.service.storage

import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.IdeManager
import com.intellij.structure.domain.IdeVersion
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.misc.extractTo
import org.slf4j.LoggerFactory
import java.io.File

/**
 * @author Sergey Patrikeev
 */
interface IIdeFilesManager {

  fun <R> locked(block: () -> R): R

  fun ideList(): List<IdeVersion>

  fun getIde(version: IdeVersion): IIdeLock?

  fun addIde(ideFile: File): Boolean

  fun deleteIde(version: IdeVersion)

  interface IIdeLock {
    fun release()
  }

}

//TODO: improve IDE cache on high concurrency: don't recreate IDE instance after each lock release.
object IdeFilesManager : IIdeFilesManager {

  private val LOG = LoggerFactory.getLogger(IdeFilesManager::class.java)

  private val ideCache: MutableMap<IdeVersion, Ide> = hashMapOf()
  private val lockedIdes: MutableMap<IdeVersion, Int> = hashMapOf()
  private val deleteQueue: MutableSet<IdeVersion> = hashSetOf()

  class IdeLock(val ide: Ide) : IIdeFilesManager.IIdeLock {
    override fun release() {
      releaseLock(this)
    }
  }

  @Synchronized
  private fun releaseLock(lock: IdeLock) {
    val version = lock.ide.version
    var cnt = lockedIdes.getOrElse(version, { throw AssertionError("Unregistered lock!") })
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
  override fun <R> locked(block: () -> R): R = block()

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
  override fun ideList(): List<IdeVersion> = FileManager.getFilesOfType(FileType.IDE).map { it -> IdeVersion.createIdeVersion(it.name) }.toList()

  @Synchronized
  override fun getIde(version: IdeVersion): IdeLock? {
    val ideFile = FileManager.getFileByName(version.asString(), FileType.IDE)
    if (!ideFile.isDirectory) {
      return null
    }

    val ide = ideCache.getOrPut(version, { IdeManager.getInstance().createIde(ideFile) })
    val cnt = lockedIdes.getOrPut(version, { 0 })
    lockedIdes.put(version, cnt + 1)
    return IdeLock(ide)
  }

  @Synchronized
  override fun deleteIde(version: IdeVersion) {
    LOG.info("Deleting IDE #$version")
    deleteQueue.add(version)
    if (!lockedIdes.contains(version)) {
      onRelease(version)
    }
  }

  @Synchronized
  override fun addIde(ideFile: File): Boolean {
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