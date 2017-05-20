package com.jetbrains.pluginverifier.verifiers.instruction

import com.jetbrains.pluginverifier.problems.*
import com.jetbrains.pluginverifier.reference.SymbolicReference
import com.jetbrains.pluginverifier.utils.*
import org.jetbrains.intellij.plugins.internal.asm.Opcodes
import org.jetbrains.intellij.plugins.internal.asm.tree.*

/**
 * Verifies GETSTATIC, PUTSTATIC, GETFIELD and PUTFIELD instructions
 *
 * @author Sergey Patrikeev
 */
class FieldAccessInstructionVerifier : InstructionVerifier {

  override fun verify(clazz: ClassNode, method: MethodNode, instr: AbstractInsnNode, ctx: VerificationContext) {
    if (instr is FieldInsnNode) {
      FieldsImplementation(clazz, method, instr, ctx).verify()
    }
  }

}

private class FieldsImplementation(val verifiableClass: ClassNode,
                                   val verifiableMethod: MethodNode,
                                   val instr: FieldInsnNode,
                                   val ctx: VerificationContext,
                                   val fieldOwner: String = instr.owner,
                                   val fieldName: String = instr.name,
                                   val fieldDescriptor: String = instr.desc) {

  val instruction: Instruction = when (instr.opcode) {
    Opcodes.PUTFIELD -> Instruction.PUT_FIELD
    Opcodes.GETFIELD -> Instruction.GET_FIELD
    Opcodes.PUTSTATIC -> Instruction.PUT_STATIC
    Opcodes.GETSTATIC -> Instruction.GET_STATIC
    else -> throw IllegalArgumentException()
  }

  fun verify() {
    when (instruction) {
      Instruction.PUT_FIELD -> processPutField()
      Instruction.GET_FIELD -> processGetField()
      Instruction.PUT_STATIC -> processPutStatic()
      Instruction.GET_STATIC -> processGetStatic()
      else -> throw IllegalArgumentException()
    }
  }

  fun processPutField() {
    val found = resolveField() ?: return

    /*
    Otherwise, if the resolved field is a static field, putfield throws an IncompatibleClassChangeError.
     */
    if (VerifierUtil.isStatic(found.fieldNode)) {
      val fieldDeclaration = ctx.fromField(found.definingClass, found.fieldNode)
      ctx.registerProblem(NonStaticAccessOfStaticFieldProblem(fieldDeclaration, getFromMethod(), Instruction.PUT_FIELD))
    }

    /*
    Otherwise, if the field is final, it must be declared in the current class, and the instruction must occur in
     an instance initialization method (<init>) of the current class. Otherwise, an IllegalAccessError is thrown.
    */

    /*
    This check is according to the JVM 8 spec, but Kotlin and others violate it (Java 8 doesn't complain too)
    if (!(StringUtil.equals(location.getClassNode().name, verifiedClass.name) && "<init>".equals(verifierMethod.name))) {
    */
    if (VerifierUtil.isFinal(found.fieldNode)) {
      if (found.definingClass.name != verifiableClass.name) {
        val fieldDeclaration = ctx.fromField(found.definingClass, found.fieldNode)
        val accessor = getFromMethod()
        ctx.registerProblem(ChangeFinalFieldProblem(fieldDeclaration, accessor, Instruction.PUT_FIELD))
      }
    }
  }

  fun processGetField() {
    val found = resolveField() ?: return

    //Otherwise, if the resolved field is a static field, getfield throws an IncompatibleClassChangeError.
    if (VerifierUtil.isStatic(found.fieldNode)) {
      val fieldDeclaration = ctx.fromField(found.definingClass, found.fieldNode)
      ctx.registerProblem(NonStaticAccessOfStaticFieldProblem(fieldDeclaration, getFromMethod(), Instruction.GET_FIELD))
    }
  }

  fun processPutStatic() {
    val found = resolveField() ?: return

    //Otherwise, if the resolved field is not a static (class) field or an interface field, putstatic throws an IncompatibleClassChangeError.
    if (!VerifierUtil.isStatic(found.fieldNode)) {
      val fieldDeclaration = ctx.fromField(found.definingClass, found.fieldNode)
      val methodLocation = getFromMethod()
      ctx.registerProblem(StaticAccessOfNonStaticFieldProblem(fieldDeclaration, methodLocation, Instruction.PUT_STATIC))
    }

    /*
    Otherwise, if the field is final, it must be declared in the current class, and the instruction
    must occur in the <clinit> method of the current class. Otherwise, an IllegalAccessError is thrown.

    if (!(StringUtil.equals(location.getClassNode().name, verifiedClass.name) && "<clinit>".equals(verifierMethod.name))) {
    */
    if (VerifierUtil.isFinal(found.fieldNode)) {
      if (found.definingClass.name != verifiableClass.name) {
        val fieldDeclaration = ctx.fromField(found.definingClass, found.fieldNode)
        val accessor = getFromMethod()
        ctx.registerProblem(ChangeFinalFieldProblem(fieldDeclaration, accessor, Instruction.PUT_STATIC))
      }
    }

  }

  fun processGetStatic() {
    val found = resolveField() ?: return

    //Otherwise, if the resolved field is not a static (class) field or an interface field, getstatic throws an IncompatibleClassChangeError.
    if (!VerifierUtil.isStatic(found.fieldNode)) {
      val fieldDeclaration = ctx.fromField(found.definingClass, found.fieldNode)
      val methodLocation = getFromMethod()
      ctx.registerProblem(StaticAccessOfNonStaticFieldProblem(fieldDeclaration, methodLocation, Instruction.GET_STATIC))
    }
  }


  private fun checkFieldIsAccessible(location: ResolvedField) {
    val definingClass = location.definingClass
    val fieldNode = location.fieldNode

    var accessProblem: AccessType? = null

    when {
      VerifierUtil.isPrivate(fieldNode) ->
        if (verifiableClass.name != definingClass.name) {
          //accessing to the private field of the other class
          accessProblem = AccessType.PRIVATE
        }
      VerifierUtil.isProtected(fieldNode) -> {
        if (!VerifierUtil.haveTheSamePackage(verifiableClass, definingClass)) {
          if (!ctx.isSubclassOf(verifiableClass, definingClass)) {
            accessProblem = AccessType.PROTECTED
          }
        }
      }
      VerifierUtil.isDefaultAccess(fieldNode) ->
        if (!VerifierUtil.haveTheSamePackage(verifiableClass, definingClass)) {
          accessProblem = AccessType.PACKAGE_PRIVATE
        }
    }

    if (accessProblem != null) {
      val fieldDeclaration = ctx.fromField(location.definingClass, location.fieldNode)
      ctx.registerProblem(IllegalFieldAccessProblem(fieldDeclaration, getFromMethod(), instruction, accessProblem))
    }
  }

  /**
   *  To resolve an unresolved symbolic reference from D to a field in a class or interface C,
   *  the symbolic reference to C given by the field reference must first be resolved (§5.4.3.1).
   *
   *  Therefore, any exception that can be thrown as a result of failure of resolution of a class or interface
   *  reference can be thrown as a result of failure of field resolution.
   *
   *  If the reference to C can be successfully resolved, an exception relating
   *  to the failure of resolution of the field reference itself can be thrown.
   */
  private fun resolveField(): ResolvedField? {
    if (fieldOwner.startsWith("[")) {
      //check that the array type exists
      val arrayType = VerifierUtil.extractClassNameFromDescr(fieldOwner)
      if (arrayType != null) {
        ctx.checkClassExistsOrExternal(arrayType, { getFromMethod() })
      }
      return null
    }

    val resolveClass = ctx.resolveClassOrProblem(fieldOwner, verifiableClass, { getFromMethod() }) ?: return null

    val (fail, resolvedField) = resolveFieldSteps(resolveClass)
    if (fail) {
      return null
    }
    if (resolvedField == null) {
      val fieldReference = SymbolicReference.fieldOf(fieldOwner, fieldName, fieldDescriptor)
      ctx.registerProblem(FieldNotFoundProblem(fieldReference, getFromMethod(), instruction))
    } else {
      checkFieldIsAccessible(resolvedField)
    }
    return resolvedField
  }


  fun getFromMethod() = ctx.fromMethod(verifiableClass, verifiableMethod)

  data class LookupResult(val fail: Boolean, val resolvedField: ResolvedField?)

  companion object {
    val NOT_FOUND = LookupResult(false, null)
    val FAILED_LOOKUP = LookupResult(true, null)
  }

  @Suppress("UNCHECKED_CAST")
  fun resolveFieldSteps(currentClass: ClassNode): LookupResult {
    /**
     * 1) If C declares a field with the name and descriptor specified by the field reference,
     * field lookup succeeds. The declared field is the result of the field lookup.
     */
    val fields = currentClass.fields as List<FieldNode>
    val matching = fields.firstOrNull { it.name == fieldName && it.desc == fieldDescriptor }
    if (matching != null) {
      return LookupResult(false, ResolvedField(currentClass, matching))
    }

    /**
     * 2) Otherwise, field lookup is applied recursively to the direct superinterfaces
     * of the specified class or interface C.
     */
    for (anInterface in currentClass.interfaces as List<String>) {
      val resolvedIntf = ctx.resolveClassOrProblem(anInterface, currentClass, { ctx.fromClass(currentClass) }) ?: return FAILED_LOOKUP

      val (fail, resolvedField) = resolveFieldSteps(resolvedIntf)
      if (fail) {
        return FAILED_LOOKUP
      }
      if (resolvedField != null) {
        return LookupResult(false, resolvedField)
      }
    }

    /**
     * 3) Otherwise, if C has a superclass S, field lookup is applied recursively to S.
     */
    val superName = currentClass.superName
    if (superName != null) {
      val resolvedSuper = ctx.resolveClassOrProblem(superName, currentClass, { ctx.fromClass(currentClass) }) ?: return FAILED_LOOKUP
      val (fail, resolvedField) = resolveFieldSteps(resolvedSuper)
      if (fail) {
        return FAILED_LOOKUP
      }
      if (resolvedField != null) {
        return LookupResult(false, resolvedField)
      }
    }

    /**
     * 4) Otherwise, field lookup fails.
     */
    return NOT_FOUND
  }

  data class ResolvedField(val definingClass: ClassNode, val fieldNode: FieldNode)

}

