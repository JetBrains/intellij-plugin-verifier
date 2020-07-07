/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.utils.contentBuilder

import com.jetbrains.plugin.structure.base.utils.archiveDirectoryTo
import com.jetbrains.plugin.structure.base.utils.extension
import com.jetbrains.plugin.structure.base.utils.forceDeleteIfExists
import java.nio.file.Files
import java.nio.file.Path

class ZipSpec : ChildrenOwnerSpec {
  private val directorySpec = DirectorySpec()

  override fun addChild(name: String, spec: ContentSpec) {
    directorySpec.addChild(name, spec)
  }

  override fun generate(target: Path) {
    check(target.extension == "jar" || target.extension == "zip" || target.extension == "nupkg") {
      "Must be a jar or zip archive: $target"
    }
    val zipContentDir = Files.createTempDirectory(target.parent, "archive-content-")
    try {
      directorySpec.generate(zipContentDir)
      zipContentDir.archiveDirectoryTo(target)
    } finally {
      zipContentDir.forceDeleteIfExists()
    }
  }
}