package com.jetbrains.plugin.structure.ide.layout

import java.nio.file.Path

data class ModuleLoadingContext(val artifactPath: Path, val descriptorName: String)