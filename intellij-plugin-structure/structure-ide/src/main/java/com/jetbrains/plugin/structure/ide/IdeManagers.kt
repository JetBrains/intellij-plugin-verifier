package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode

fun createIdeManager(init: IdeManagerConfiguration.() -> Unit): IdeManager {
  val spec = IdeManagerConfiguration()
  spec.init()
  return DispatchingIdeManager(spec)
}

class IdeManagerConfiguration {
  var missingLayoutFileMode: MissingLayoutFileMode = MissingLayoutFileMode.SKIP_AND_WARN
}