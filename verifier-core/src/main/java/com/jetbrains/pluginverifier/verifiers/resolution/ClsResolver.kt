package com.jetbrains.pluginverifier.verifiers.resolution

import java.io.Closeable

interface ClsResolver : Closeable {
  fun resolveClass(className: String): ClsResolution

  fun isExternalClass(className: String): Boolean

  fun classExists(className: String): Boolean

  fun getOriginOfClass(className: String): ClassFileOrigin?
}