package com.jetbrains.plugin.structure.testUtils.contentBuilder

import java.io.File

interface ContentSpec {
  fun generate(target: File)
}
