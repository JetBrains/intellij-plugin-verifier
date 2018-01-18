package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption
import com.jetbrains.pluginverifier.results.presentation.ClassOption
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.FULL_HOST_WITH_SIGNATURE
import com.jetbrains.pluginverifier.results.presentation.HostClassOption.NO_HOST
import com.jetbrains.pluginverifier.results.presentation.MethodParameterNameOption.WITH_PARAM_NAMES_IF_AVAILABLE
import com.jetbrains.pluginverifier.results.presentation.MethodParameterTypeOption.SIMPLE_PARAM_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.MethodReturnTypeOption.SIMPLE_RETURN_TYPE_CLASS_NAME
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation
import com.jetbrains.pluginverifier.results.presentation.formatMethodLocation

data class MethodNotImplementedProblem(val abstractMethod: MethodLocation,
                                       val incompleteClass: ClassLocation) : CompatibilityProblem() {

  override val shortDescription = "Abstract method ${abstractMethod.formatMethodLocation(FULL_HOST_WITH_SIGNATURE, SIMPLE_PARAM_CLASS_NAME, SIMPLE_RETURN_TYPE_CLASS_NAME, WITH_PARAM_NAMES_IF_AVAILABLE)} is not implemented"

  override val fullDescription = "Non-abstract class ${incompleteClass.formatClassLocation(ClassOption.FULL_NAME, ClassGenericsSignatureOption.NO_GENERICS)} inherits from ${abstractMethod.hostClass.formatClassLocation(ClassOption.FULL_NAME, ClassGenericsSignatureOption.WITH_GENERICS)} but doesn't implement the abstract method ${abstractMethod.formatMethodLocation(NO_HOST, SIMPLE_PARAM_CLASS_NAME, SIMPLE_RETURN_TYPE_CLASS_NAME, WITH_PARAM_NAMES_IF_AVAILABLE)}. This can lead to **AbstractMethodError** exception at runtime."
}