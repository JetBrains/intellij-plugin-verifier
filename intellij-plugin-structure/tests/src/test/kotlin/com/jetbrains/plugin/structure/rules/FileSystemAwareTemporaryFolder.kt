package com.jetbrains.plugin.structure.rules

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.jetbrains.plugin.structure.base.utils.create
import com.jetbrains.plugin.structure.base.utils.createDir
import com.jetbrains.plugin.structure.base.utils.forceDeleteIfExists
import org.junit.rules.ExternalResource
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path

class FileSystemAwareTemporaryFolder(private val fileSystemType: FileSystemType) : ExternalResource() {
  private var fileSystem: FileSystem? = null
  lateinit var root: Path

  @Throws(Throwable::class)
  override fun before() {
    when (fileSystemType) {
      FileSystemType.IN_MEMORY -> {
        Jimfs.newFileSystem(Configuration.unix()).also {
          fileSystem = it
          root = Files.createTempDirectory(it.getPath("tmp").createDir(), "junit")
        }
      }
      FileSystemType.DEFAULT -> {
        root = Files.createTempDirectory("junit")
      }
    }
  }

  override fun after() {
    if (System.getenv("TEAMCITY_VERSION") == null) {
      root.forceDeleteIfExists()
    }
    fileSystem?.close()
  }

  fun newFile(fileName: String) = root.resolve(fileName).create()

  fun newFile() = Files.createTempFile(root, "junit", null)

  fun newFolder(folder: String): Path = root.resolve(folder).createDir()

  fun newFolder(): Path = Files.createTempDirectory(root, "")
}

enum class FileSystemType {
  IN_MEMORY,
  DEFAULT
}