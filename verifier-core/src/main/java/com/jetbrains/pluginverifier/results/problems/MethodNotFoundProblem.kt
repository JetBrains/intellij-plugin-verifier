package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.FULL_HOST_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodParameterTypeOption.FULL_PARAM_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodReturnTypeOption.FULL_RETURN_TYPE_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodReturnTypeOption.SIMPLE_RETURN_TYPE_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.formatMethodLocation
import com.jetbrains.pluginverifier.results.presentation.formatMethodReference
import com.jetbrains.pluginverifier.results.reference.MethodReference

data class MethodNotFoundProblem(val method: MethodReference,
                                 val caller: MethodLocation,
                                 val instruction: Instruction) : Problem() {

  override val shortDescription = "Invocation of unresolved method {0}".formatMessage(method.formatMethodReference(FULL_HOST_NAME, SIMPLE_PARAM_CLASS_NAME, SIMPLE_RETURN_TYPE_CLASS_NAME))

  override val fullDescription = ("Method {0} contains an *{1}* instruction referencing an unresolved method {2}. " +
      "This can lead to **NoSuchMethodError** exception at runtime.").formatMessage(caller.formatMethodLocation(FULL_HOST_NAME, FULL_PARAM_CLASS_NAME, FULL_RETURN_TYPE_CLASS_NAME), instruction, method)

}