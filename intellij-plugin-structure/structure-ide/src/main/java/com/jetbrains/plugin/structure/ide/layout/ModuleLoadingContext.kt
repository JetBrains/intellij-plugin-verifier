package com.jetbrains.plugin.structure.ide.layout

import com.jetbrains.plugin.structure.base.utils.exists
import java.nio.file.Path

internal data class ModuleLoadingContext(val artifactPath: Path, val descriptorName: String) {
  fun exists(): Boolean = artifactPath.exists()
}