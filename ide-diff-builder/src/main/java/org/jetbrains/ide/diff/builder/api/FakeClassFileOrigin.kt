package org.jetbrains.ide.diff.builder.api

import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileOrigin
import java.nio.file.Path
import java.nio.file.Paths

object FakeClassFileOrigin : ClassFileOrigin {
  override val classPath: Path
    get() = Paths.get("fake")
}
