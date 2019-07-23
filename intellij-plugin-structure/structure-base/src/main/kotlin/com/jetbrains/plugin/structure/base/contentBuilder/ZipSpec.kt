package com.jetbrains.plugin.structure.base.contentBuilder

import com.jetbrains.plugin.structure.base.utils.archiveDirectory
import com.jetbrains.plugin.structure.base.utils.forceDeleteIfExists
import java.io.File

class ZipSpec : ChildrenOwnerSpec {
  private val directorySpec = DirectorySpec()

  override fun addChild(name: String, spec: ContentSpec) {
    directorySpec.addChild(name, spec)
  }

  override fun generate(target: File) {
    check(target.extension == "jar" || target.extension == "zip") { "Must be a jar or zip archive: $target" }
    val zipContentDir = createTempDir()
    try {
      directorySpec.generate(zipContentDir)
      archiveDirectory(zipContentDir, target, includeDirectory = false, includeEmptyDirectories = true)
    } finally {
      zipContentDir.forceDeleteIfExists()
    }
  }
}