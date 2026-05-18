package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.plugin.structure.classes.utils.KtClassResolver
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.filter.ApiUsageFilter
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileAsm
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.AbstractInsnNode

/**
 * Allows invocations whose target cannot be overridden, mirroring the IntelliJ Platform
 * inspection's `target.isOverridable()` guard.
 *
 * Covers two shapes:
 *  - Static methods (JVM `ACC_STATIC`). For Kotlin this matches `@JvmStatic` accessors.
 *  - Instance methods whose declaring class is a Kotlin `object` or `companion object`.
 *    Those compile to non-static methods on a singleton holder, so the static flag alone
 *    does not catch them, but the singleton cannot be subclassed and the
 *    `@ApiStatus.OverrideOnly` annotation cannot be enforced at the call site.
 */
class NonOverridableMethodUsageFilter : ApiUsageFilter {
  private val ktClassResolver = KtClassResolver()

  override fun allow(
    invokedMethod: Method,
    invocationInstruction: AbstractInsnNode,
    callerMethod: Method,
    context: VerificationContext
  ): Boolean = invokedMethod.isStatic || invokedMethod.containingClassFile.isKotlinObject

  private val ClassFile.isKotlinObject: Boolean
    get() {
      val asmNode = (this as? ClassFileAsm)?.asmNode ?: return false
      return ktClassResolver[asmNode]?.isObject == true
    }
}
