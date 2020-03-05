package com.jetbrains.plugin.structure.base.utils.contentBuilder

import java.io.File

interface ContentSpec {
  fun generate(target: File)
}
