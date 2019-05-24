package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.results.location.Location

interface ClassFileMember {
  val containingClassFile: ClassFile

  val location: Location
}