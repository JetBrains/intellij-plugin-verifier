package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.classes.resolvers.JarFileSystemsPool.MAX_OPEN_JAR_FILE_SYSTEMS
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.time.Clock
import java.time.Instant

/**
 * Application-level object managing open jar file systems.
 *
 * No more than [MAX_OPEN_JAR_FILE_SYSTEMS] will be open in the running application simultaneously.
 */
@Deprecated("Use 'JarFileSystemProvider' implementations")
internal object JarFileSystemsPool {
  private const val MAX_OPEN_JAR_FILE_SYSTEMS = 256

  private const val UNUSED_JAR_FILE_SYSTEMS_TO_CLOSE = 64

  private val openJarFileSystems = hashMapOf<Path, FSHandler>()

  private val clock = Clock.systemUTC()

  fun checkIsJar(jarPath: Path) {
    require(jarPath.exists()) { "File does not exist: $jarPath" }
    require(jarPath.simpleName.endsWith(".jar") || jarPath.simpleName.endsWith(".zip")) { "File is neither a .jar nor .zip archive: $jarPath" }
  }

  fun <T> perform(jarPath: Path, action: (FileSystem) -> T): T {
    checkIsJar(jarPath)
    val fsHandler = getOrOpenFsHandler(jarPath)
    check(fsHandler.jarFs.isOpen)
    try {
      return action(fsHandler.jarFs)
    } finally {
      release(fsHandler)
    }
  }

  @Synchronized
  private fun getOrOpenFsHandler(jarPath: Path): FSHandler {
    val fsHandler = openJarFileSystems.getOrPut(jarPath) {
      val jarFs = FileSystems.newFileSystem(jarPath, JarFileSystemsPool::class.java.classLoader)
      FSHandler(jarFs, clock.instant(), 0)
    }

    fsHandler.users++
    fsHandler.lastAccessTime = clock.instant()

    if (openJarFileSystems.size > MAX_OPEN_JAR_FILE_SYSTEMS) {
      val toCloseEntries = openJarFileSystems.entries
        .filter { it.value.users == 0 }
        .sortedBy { it.value.lastAccessTime }
        .take(UNUSED_JAR_FILE_SYSTEMS_TO_CLOSE)
      toCloseEntries.forEach { (path, fsHandler) ->
        fsHandler.jarFs.closeLogged()
        openJarFileSystems.remove(path)
      }
    }

    return fsHandler
  }

  @Synchronized
  fun close(jarPath: Path) {
    val fsHandler = openJarFileSystems[jarPath] ?: return
    if (fsHandler.users == 0) {
      fsHandler.jarFs.closeLogged()
      openJarFileSystems.remove(jarPath)
    }
  }

  @Synchronized
  private fun release(fsHandler: FSHandler) {
    fsHandler.users--
  }

  private data class FSHandler(
    val jarFs: FileSystem,
    var lastAccessTime: Instant,
    var users: Int
  )
}