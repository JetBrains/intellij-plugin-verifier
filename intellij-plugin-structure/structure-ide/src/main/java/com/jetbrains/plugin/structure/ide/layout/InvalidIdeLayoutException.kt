package com.jetbrains.plugin.structure.ide.layout

import java.nio.file.Path

open class InvalidIdeLayoutException(message: String) : RuntimeException(message)

class MissingClasspathFileInLayoutComponentException private constructor(message: String) :
  InvalidIdeLayoutException(message) {
  companion object {
    fun of(idePath: Path, failedComponents: List<ResolvedLayoutComponent>): InvalidIdeLayoutException {
      val failedNames = failedComponents.joinToString { it.name }
      return MissingClasspathFileInLayoutComponentException("Invalid IDE layout in [$idePath]. " +
        "The following components have missing files in 'classpath': [$failedNames]")
    }
  }
}

