package com.jetbrains.pluginverifier.verifiers.instruction

import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.problems.ChangeFinalFieldProblem
import com.jetbrains.pluginverifier.results.problems.InstanceAccessOfStaticFieldProblem
import com.jetbrains.pluginverifier.results.problems.StaticAccessOfInstanceFieldProblem
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.extractClassNameFromDescriptor
import com.jetbrains.pluginverifier.verifiers.resolution.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode

/**
 * Verifies `getstatic`, `putstatic`, `getfield`, `putfield` instructions.
 */
class FieldAccessInstructionVerifier : InstructionVerifier {

  override fun verify(method: Method, instructionNode: AbstractInsnNode, context: VerificationContext) {
    if (instructionNode !is FieldInsnNode) return
    val instruction = Instruction.fromOpcode(instructionNode.opcode) ?: throw IllegalArgumentException()

    val fieldOwner = instructionNode.owner
    if (fieldOwner.startsWith("[")) {
      val arrayType = fieldOwner.extractClassNameFromDescriptor()
      if (arrayType != null) {
        context.classResolver.resolveClassChecked(arrayType, method, context)
      }
      return
    }

    val ownerFile = context.classResolver.resolveClassChecked(fieldOwner, method, context)
    if (ownerFile != null) {
      val fieldReference = FieldReference(fieldOwner, instructionNode.name, instructionNode.desc)
      FieldAccessInstructionVerifierImpl(method.containingClassFile, ownerFile, fieldReference, context, instruction, method).verify()
    }
  }

}

private class FieldAccessInstructionVerifierImpl(
    val verifiedClass: ClassFile,
    val fieldOwnerClass: ClassFile,
    val fieldReference: FieldReference,
    val context: VerificationContext,
    val instruction: Instruction,
    val callerMethod: Method
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
      context.problemRegistrar.registerProblem(InstanceAccessOfStaticFieldProblem(field.location, callerMethod.location, Instruction.PUT_FIELD))
    }

    /*
    Otherwise, if the field is final, it must be declared in the current class, and the instruction must occur in
     an instance initialization method (<init>) of the current class. Otherwise, an IllegalAccessError is thrown.
    */

    if (field.isFinal) {
      if (field.containingClassFile.name != verifiedClass.name) {
        context.problemRegistrar.registerProblem(ChangeFinalFieldProblem(field.location, callerMethod.location, Instruction.PUT_FIELD))
      }
    }
  }

  private fun processGetField() {
    val field = resolveField() ?: return

    //Otherwise, if the resolved field is a static field, getfield throws an IncompatibleClassChangeError.
    if (field.isStatic) {
      context.problemRegistrar.registerProblem(InstanceAccessOfStaticFieldProblem(field.location, callerMethod.location, Instruction.GET_FIELD))
    }
  }

  private fun processPutStatic() {
    val field = resolveField() ?: return

    //Otherwise, if the resolved field is not a static (class) field or an interface field, putstatic throws an IncompatibleClassChangeError.
    if (!field.isStatic) {
      context.problemRegistrar.registerProblem(StaticAccessOfInstanceFieldProblem(field.location, callerMethod.location, Instruction.PUT_STATIC))
    }

    /*
    Otherwise, if the field is final, it must be declared in the current class, and the instruction
    must occur in the <clinit> method of the current class. Otherwise, an IllegalAccessError is thrown.
    */
    if (field.isFinal) {
      if (field.containingClassFile.name != verifiedClass.name) {
        context.problemRegistrar.registerProblem(ChangeFinalFieldProblem(field.location, callerMethod.location, Instruction.PUT_STATIC))
      }
    }

  }

  private fun processGetStatic() {
    val field = resolveField() ?: return

    //Otherwise, if the resolved field is not a static (class) field or an interface field, getstatic throws an IncompatibleClassChangeError.
    if (!field.isStatic) {
      context.problemRegistrar.registerProblem(StaticAccessOfInstanceFieldProblem(field.location, callerMethod.location, Instruction.GET_STATIC))
    }
  }

  private fun resolveField(): Field? {
    val field = FieldResolver().resolveField(fieldOwnerClass, fieldReference, context, callerMethod, instruction)
    if (field != null) {
      val usageLocation = callerMethod.location
      context.apiUsageProcessors.forEach { it.processApiUsage(field, usageLocation, context) }
    }
    return field
  }
}