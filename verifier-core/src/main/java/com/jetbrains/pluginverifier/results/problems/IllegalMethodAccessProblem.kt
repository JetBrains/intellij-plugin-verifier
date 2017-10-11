package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.access.AccessType
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.MethodLocation

data class IllegalMethodAccessProblem(val method: MethodLocation,
                                      val caller: MethodLocation,
                                      val instruction: Instruction,
                                      val methodAccess: AccessType) : Problem() {

  override val shortDescription = "Illegal invocation of {0} method {1}".formatMessage(methodAccess, method)

  override val fullDescription = "Method {0} contains an *{1}* instruction referencing a {2} method {3} that a class {4} doesn''t have access to. This can lead to **IllegalAccessError** exception at runtime.".formatMessage(caller, instruction, methodAccess, method, caller.hostClass)
}