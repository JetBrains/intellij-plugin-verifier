package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.misc.formatMessage
import com.jetbrains.pluginverifier.results.hierarchy.ClassHierarchy
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.presentation.*
import com.jetbrains.pluginverifier.results.reference.FieldReference
import java.util.*

class FieldNotFoundProblem(
    val unresolvedField: FieldReference,
    val accessor: MethodLocation,
    val fieldOwnerHierarchy: ClassHierarchy,
    val instruction: Instruction
) : CompatibilityProblem() {

  override val shortDescription = "Access to unresolved field {0}".formatMessage(unresolvedField)

  private val descriptionMainPart = buildString {
    append("Method {0} contains a *{1}* instruction referencing an unresolved field {2}. ".formatMessage(
        accessor.formatMethodLocation(
            HostClassOption.FULL_HOST_NAME,
            MethodParameterTypeOption.FULL_PARAM_CLASS_NAME,
            MethodReturnTypeOption.FULL_RETURN_TYPE_CLASS_NAME,
            MethodParameterNameOption.WITH_PARAM_NAMES_IF_AVAILABLE
        ),
        instruction,
        unresolvedField.formatFieldReference(HostClassOption.FULL_HOST_NAME, FieldTypeOption.FULL_TYPE)
    ))
    append("This can lead to **NoSuchFieldError** exception at runtime.")
  }

  override val fullDescription = buildString {
    append(descriptionMainPart)
    //Non-static fields can only be declared in super classes.
    val canBeDeclaredInSuperInterface = instruction == Instruction.GET_STATIC || instruction == Instruction.PUT_STATIC
    append(HierarchicalProblemsDescription.presentableElementMightHaveBeenDeclaredInIdeSuperTypes("field", fieldOwnerHierarchy, true, canBeDeclaredInSuperInterface))
  }

  override fun equals(other: Any?) = other is FieldNotFoundProblem
      && unresolvedField == other.unresolvedField
      && accessor == other.accessor
      && instruction == other.instruction

  override fun hashCode() = Objects.hash(unresolvedField, accessor, instruction)
}