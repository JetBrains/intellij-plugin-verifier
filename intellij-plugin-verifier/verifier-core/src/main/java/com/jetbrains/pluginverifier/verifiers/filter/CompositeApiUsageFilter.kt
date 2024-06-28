package com.jetbrains.pluginverifier.verifiers.filter

import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.AbstractInsnNode

class CompositeApiUsageFilter(private val apiUsageFilters: List<ApiUsageFilter>) : ApiUsageFilter {
  constructor(vararg filters: ApiUsageFilter) : this(filters.toList())

  override fun allow(invokedMethod: Method, invocationInstruction: AbstractInsnNode, callerMethod: Method, context: VerificationContext): Boolean {
    return apiUsageFilters.any {
      it.allow(invokedMethod, invocationInstruction, callerMethod, context)
    }
  }
}