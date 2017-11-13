package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.hierarchy.ClassHierarchy
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.presentation.*
import com.jetbrains.pluginverifier.results.reference.FieldReference

data class FieldNotFoundProblem(val field: FieldReference,
                                val accessor: MethodLocation,
                                val fieldOwnerHierarchy: ClassHierarchy,
                                val instruction: Instruction,
                                val ideVersion: IdeVersion) : Problem() {

  override val shortDescription = "Access to unresolved field {0}".formatMessage(field)

  private val descriptionMainPart = buildString {
    append("Method {0} contains a *{1}* instruction referencing an unresolved field {2}. ".formatMessage(
        accessor.formatMethodLocation(HostClassOption.FULL_HOST_NAME, MethodParameterTypeOption.FULL_PARAM_CLASS_NAME, MethodReturnTypeOption.FULL_RETURN_TYPE_CLASS_NAME, MethodParameterNameOption.WITH_PARAM_NAMES_IF_AVAILABLE),
        instruction,
        field
    ))
    append("This can lead to **NoSuchFieldError** exception at runtime.")
  }

  override val fullDescription = buildString {
    append(descriptionMainPart)
    //Non-static fields can only be declared in super classes.
    val canBeDeclaredInSuperInterface = instruction == Instruction.GET_STATIC || instruction == Instruction.PUT_STATIC
    append(HierarchicalProblemsDescription.presentableElementMightHaveBeenDeclaredInIdeSuperTypes("field", fieldOwnerHierarchy, ideVersion, true, canBeDeclaredInSuperInterface))
  }

  override val equalityReference: String
    get() = descriptionMainPart
}