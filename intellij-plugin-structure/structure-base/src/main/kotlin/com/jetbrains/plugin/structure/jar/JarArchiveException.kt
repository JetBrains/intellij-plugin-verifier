package com.jetbrains.plugin.structure.jar

open class JarArchiveException : RuntimeException {
  constructor(message: String) : super(message)
  constructor(message: String, cause: Throwable) : super(message, cause)
}
