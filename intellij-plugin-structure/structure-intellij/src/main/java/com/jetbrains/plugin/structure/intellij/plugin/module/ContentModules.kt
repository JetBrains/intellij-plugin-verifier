package com.jetbrains.plugin.structure.intellij.plugin.module

import com.jetbrains.plugin.structure.intellij.plugin.Classpath
import java.nio.file.Path

data class ContentModules(val pluginArtifact: Path, val modules: List<ContentModule>) {
  val resolvedClassPath: List<Path>
    get() = modules.map { it.artifactPath }

  fun asClasspath(): Classpath = Classpath.of(resolvedClassPath)
}