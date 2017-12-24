package com.jetbrains.pluginverifier.misc

fun <T : Throwable> Throwable.findCause(klass: Class<T>): T? {
  var e: Throwable? = this
  while (e != null && !klass.isInstance(e)) {
    e = e.cause
  }
  @Suppress("UNCHECKED_CAST")
  return e as? T
}

fun <T : Throwable> Throwable.causedBy(klass: Class<T>): Boolean = findCause(klass) != null