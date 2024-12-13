/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode
import java.nio.file.Path

fun createIdeManager(init: IdeManagerConfiguration.() -> Unit): IdeManager {
  val spec = IdeManagerConfiguration()
  spec.init()
  return DispatchingIdeManager(spec)
}

fun createIde(init: IdeConfiguration.() -> Unit): Ide {
  val spec = IdeConfiguration()
  spec.init()
  val idePath = spec.path
  require(idePath != null) { "IDE Path must be set" }
  val ideManagerConfig = IdeManagerConfiguration(spec.missingLayoutFileMode)
  return DispatchingIdeManager(ideManagerConfig).createIde(idePath)
}

class IdeManagerConfiguration(var missingLayoutFileMode: MissingLayoutFileMode = MissingLayoutFileMode.SKIP_AND_WARN)

class IdeConfiguration {
  var missingLayoutFileMode: MissingLayoutFileMode = MissingLayoutFileMode.SKIP_AND_WARN
  var path: Path? = null
}