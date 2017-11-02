package com.jetbrains.pluginverifier.results.location

import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.results.presentation.HostClassOption
import com.jetbrains.pluginverifier.results.presentation.MethodParameterNameOption.WITH_PARAM_NAMES_IF_AVAILABLE
import com.jetbrains.pluginverifier.results.presentation.MethodParameterTypeOption
import com.jetbrains.pluginverifier.results.presentation.MethodReturnTypeOption
import com.jetbrains.pluginverifier.results.presentation.formatMethodLocation

data class MethodLocation(val hostClass: ClassLocation,
                          val methodName: String,
                          val methodDescriptor: String,
                          val parameterNames: List<String>,
                          val signature: String,
                          val modifiers: Modifiers) : Location {

  override fun toString(): String = formatMethodLocation(HostClassOption.FULL_HOST_WITH_SIGNATURE, MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME, MethodReturnTypeOption.SIMPLE_RETURN_TYPE_CLASS_NAME, WITH_PARAM_NAMES_IF_AVAILABLE)
}