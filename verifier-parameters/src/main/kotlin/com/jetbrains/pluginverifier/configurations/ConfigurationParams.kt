package com.jetbrains.pluginverifier.configurations

import java.io.Closeable

interface ConfigurationParams : Closeable {
  fun presentableText(): String
}
