package com.jetbrains.pluginverifier.repository.resources

import org.slf4j.Logger

fun Logger.debugMaybe(message: () -> String) {
  if (isDebugEnabled) {
    debug(message())
  }
}