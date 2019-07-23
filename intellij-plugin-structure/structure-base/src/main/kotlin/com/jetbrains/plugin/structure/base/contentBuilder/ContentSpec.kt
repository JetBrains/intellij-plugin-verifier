package com.jetbrains.plugin.structure.base.contentBuilder

import java.io.File

interface ContentSpec {
  fun generate(target: File)
}
