package com.jetbrains.pluginverifier.configurations

import java.io.Closeable

interface TaskParameters : Closeable {
  fun presentableText(): String
}
