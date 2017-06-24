package com.jetbrains.pluginverifier.tasks

import java.io.Closeable

interface TaskParameters : Closeable {
  fun presentableText(): String
}
