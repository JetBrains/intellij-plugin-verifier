package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileOrigin
import com.jetbrains.pluginverifier.verifiers.resolution.ClassResolution
import com.jetbrains.pluginverifier.verifiers.resolution.ClassResolver

object EmptyClassResolver : ClassResolver {
  override fun resolveClass(className: String) = ClassResolution.NotFound

  override fun isExternalClass(className: String) = false

  override fun classExists(className: String) = false

  override fun packageExists(packageName: String) = false

  override fun getOriginOfClass(className: String): ClassFileOrigin? = null

  override fun close() = Unit
}