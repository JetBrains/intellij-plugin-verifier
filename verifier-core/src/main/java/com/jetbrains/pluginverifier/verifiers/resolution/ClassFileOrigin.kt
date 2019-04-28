package com.jetbrains.pluginverifier.verifiers.resolution

import java.nio.file.Path

interface ClassFileOrigin {
  val classPath: Path
}