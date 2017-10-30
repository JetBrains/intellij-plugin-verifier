package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.presentation.*

data class OverridingFinalMethodProblem(val finalMethod: MethodLocation,
                                        val invalidClass: ClassLocation) : Problem() {

  override val shortDescription = "Overriding a final method ${finalMethod.formatMethodLocation(HostClassOption.FULL_HOST_NAME, MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME, MethodReturnTypeOption.SIMPLE_RETURN_TYPE_CLASS_NAME)}"

  override val fullDescription = "Class ${invalidClass.formatClassLocation(ClassOption.FULL_NAME, ClassGenericsSignatureOption.WITH_GENERICS)} overrides the final method ${finalMethod.formatMethodLocation(HostClassOption.FULL_HOST_NAME, MethodParameterTypeOption.FULL_PARAM_CLASS_NAME, MethodReturnTypeOption.FULL_RETURN_TYPE_CLASS_NAME)}. This can lead to **VerifyError** exception at runtime."
}