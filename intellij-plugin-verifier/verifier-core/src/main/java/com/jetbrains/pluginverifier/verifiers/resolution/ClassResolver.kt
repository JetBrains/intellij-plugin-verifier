package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import java.io.Closeable

/**
 * Implements class resolution strategy depending on type of performed verification.
 */
interface ClassResolver : Closeable {

  fun resolveClassOrNull(className: String): ClassFile?

  fun resolveClassChecked(className: String, referrer: ClassFileMember, context: VerificationContext): ClassFile?

  fun packageExists(packageName: String): Boolean
}