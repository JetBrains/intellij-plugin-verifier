package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.ClassResolver

object EmptyClassResolver : ClassResolver {
  override fun resolveClassOrNull(className: String): ClassFile? = null

  override fun resolveClassChecked(
      className: String,
      referrer: ClassFileMember,
      context: VerificationContext
  ): ClassFile? = null

  override fun packageExists(packageName: String) = false

  override fun close() = Unit
}