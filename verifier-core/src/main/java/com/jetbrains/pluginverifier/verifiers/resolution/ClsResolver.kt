package com.jetbrains.pluginverifier.verifiers.resolution

interface ClsResolver {
  fun resolveClass(className: String): ClsResolution

  fun isExternalClass(className: String): Boolean

  fun classExists(className: String): Boolean

  fun getOriginOfClass(className: String): ClassFileOrigin?
}