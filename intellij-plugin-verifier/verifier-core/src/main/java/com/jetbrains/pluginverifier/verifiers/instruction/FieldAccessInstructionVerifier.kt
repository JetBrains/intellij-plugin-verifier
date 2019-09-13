package com.jetbrains.pluginverifier.verifiers.instruction

import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.problems.ChangeFinalFieldProblem
import com.jetbrains.pluginverifier.results.problems.InstanceAccessOfStaticFieldProblem
import com.jetbrains.pluginverifier.results.problems.StaticAccessOfInstanceFieldProblem
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile
import com.jetbrains.pluginverifier.verifiers.resolution.Field
import com.jetbrains.pluginverifier.verifiers.resolution.FieldResolver
import com.jetbrains.pluginverifier.verifiers.resolution.Method

class FieldAccessInstructionVerifier(
    private val callerMethod: Method,
    private val fieldOwnerClass: ClassFile,
    private val fieldReference: FieldReference,
    private val context: VerificationContext,
    private val instruction: Instruction
) {

  fun verify() {
    when (instruction) {
      Instruction.PUT_FIELD -> processPutField()
      Instruction.GET_FIELD -> processGetField()
      Instruction.PUT_STATIC -> processPutStatic()
      Instruction.GET_STATIC -> processGetStatic()
      else -> throw IllegalArgumentException()
    }
  }

  private fun processPutField() {
    val field = resolveField() ?: return

    /*
    Otherwise, if the resolved field is a static field, putfield throws an IncompatibleClassChangeError.
     */
    if (field.isStatic) {
      context.problemRegistrar.registerProblem(InstanceAccessOfStaticFieldProblem(fieldReference, field.location, callerMethod.location, instruction))
    }

    /*
    Otherwise, if the field is final, it must be declared in the current class, and the instruction must occur in
     an instance initialization method (<init>) of the current class. Otherwise, an IllegalAccessError is thrown.
    */

    if (field.isFinal) {
      if (field.containingClassFile.name != callerMethod.containingClassFile.name) {
        context.problemRegistrar.registerProblem(ChangeFinalFieldProblem(fieldReference, field.location, callerMethod.location, instruction))
      }
    }
  }

  private fun processGetField() {
    val field = resolveField() ?: return

    //Otherwise, if the resolved field is a static field, getfield throws an IncompatibleClassChangeError.
    if (field.isStatic) {
      context.problemRegistrar.registerProblem(InstanceAccessOfStaticFieldProblem(fieldReference, field.location, callerMethod.location, instruction))
    }
  }

  private fun processPutStatic() {
    val field = resolveField() ?: return

    //Otherwise, if the resolved field is not a static (class) field or an interface field, putstatic throws an IncompatibleClassChangeError.
    if (!field.isStatic) {
      context.problemRegistrar.registerProblem(StaticAccessOfInstanceFieldProblem(fieldReference, field.location, callerMethod.location, instruction))
    }

    /*
    Otherwise, if the field is final, it must be declared in the current class, and the instruction
    must occur in the <clinit> method of the current class. Otherwise, an IllegalAccessError is thrown.
    */
    if (field.isFinal) {
      if (field.containingClassFile.name != callerMethod.containingClassFile.name) {
        context.problemRegistrar.registerProblem(ChangeFinalFieldProblem(fieldReference, field.location, callerMethod.location, instruction))
      }
    }

  }

  private fun processGetStatic() {
    val field = resolveField() ?: return

    //Otherwise, if the resolved field is not a static (class) field or an interface field, getstatic throws an IncompatibleClassChangeError.
    if (!field.isStatic) {
      context.problemRegistrar.registerProblem(StaticAccessOfInstanceFieldProblem(fieldReference, field.location, callerMethod.location, instruction))
    }
  }

  private fun resolveField(): Field? {
    val field = FieldResolver().resolveField(fieldOwnerClass, fieldReference, context, callerMethod, instruction)
    if (field != null) {
      val usageLocation = callerMethod.location
      context.apiUsageProcessors.forEach { it.processApiUsage(fieldReference, field, usageLocation, context) }
    }
    return field
  }
}