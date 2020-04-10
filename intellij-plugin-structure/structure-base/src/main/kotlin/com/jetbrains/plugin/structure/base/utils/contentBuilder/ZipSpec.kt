package com.jetbrains.plugin.structure.base.utils.contentBuilder

import com.jetbrains.plugin.structure.base.utils.archiveDirectoryTo
import com.jetbrains.plugin.structure.base.utils.forceDeleteIfExists
import java.io.File

class ZipSpec : ChildrenOwnerSpec {
  private val directorySpec = DirectorySpec()

  override fun addChild(name: String, spec: ContentSpec) {
    directorySpec.addChild(name, spec)
  }

  override fun generate(target: File) {
    check(target.extension == "jar" || target.extension == "zip" || target.extension == "nupkg") {
      "Must be a jar or zip archive: $target"
    }
    val zipContentDir = createTempDir()
    try {
      directorySpec.generate(zipContentDir)
      zipContentDir.archiveDirectoryTo(target)
    } finally {
      zipContentDir.forceDeleteIfExists()
    }
  }
}