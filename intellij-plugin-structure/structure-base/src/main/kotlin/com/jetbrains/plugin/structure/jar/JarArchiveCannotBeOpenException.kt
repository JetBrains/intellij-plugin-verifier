package com.jetbrains.plugin.structure.jar

import java.net.URI
import java.nio.file.Path

class JarArchiveCannotBeOpenException : JarArchiveException {
  constructor(jarUri: URI, cause: Throwable) : this("JAR file cannot be open at [$jarUri]", cause)
  constructor(jarPath: Path, cause: Throwable) : this("JAR file cannot be open at [$jarPath]", cause)
  constructor(jarPath: Path, additionalMessage: String) : super("JAR file cannot be open at [$jarPath]: $additionalMessage")
  constructor(jarPath: Path, resolvedJarUri: URI, cause: Throwable) : super("JAR file cannot be open at [$jarPath] (resolved URI: <$resolvedJarUri>)", cause)
  private constructor(message: String, cause: Throwable) : super(message, cause)
}
