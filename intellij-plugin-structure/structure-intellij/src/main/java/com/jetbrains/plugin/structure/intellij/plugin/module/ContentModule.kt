package com.jetbrains.plugin.structure.intellij.plugin.module

import java.nio.file.Path

data class ContentModule(val id: String, val artifactPath: Path, val descriptorPath: Path)