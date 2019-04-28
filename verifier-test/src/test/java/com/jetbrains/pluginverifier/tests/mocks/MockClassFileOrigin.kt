package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileOrigin
import java.nio.file.Path
import java.nio.file.Paths

object MockClassFileOrigin : ClassFileOrigin {
  override val classPath: Path
    get() = Paths.get("unused")
}