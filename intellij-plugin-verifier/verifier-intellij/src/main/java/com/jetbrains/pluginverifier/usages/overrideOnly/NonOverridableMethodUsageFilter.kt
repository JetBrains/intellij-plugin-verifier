package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.filter.ApiUsageFilter
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.AbstractInsnNode

/**
 * Allows invocations whose target cannot be overridden, mirroring the IntelliJ Platform
 * inspection's `target.isOverridable()` guard.
 *
 * Covers two shapes:
 *  - Static methods (JVM `ACC_STATIC`). For Kotlin this matches `@JvmStatic` accessors.
 *  - Instance methods on a `final` class (JVM `ACC_FINAL`). The class cannot be
 *    subclassed, so callers cannot override the method and `@ApiStatus.OverrideOnly`
 *    is unenforceable. This covers both Kotlin `object`/`companion object` (which
 *    compile to final classes) and hand-written Java final singletons.
 */
class NonOverridableMethodUsageFilter : ApiUsageFilter {
  override fun allow(
    invokedMethod: Method,
    invocationInstruction: AbstractInsnNode,
    callerMethod: Method,
    context: VerificationContext
  ): Boolean = invokedMethod.isStatic || invokedMethod.containingClassFile.isFinal
}