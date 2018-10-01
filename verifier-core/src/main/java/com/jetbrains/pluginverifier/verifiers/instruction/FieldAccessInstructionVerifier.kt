package com.jetbrains.pluginverifier.verifiers.instruction

import com.jetbrains.pluginverifier.results.access.AccessType
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedFieldUsage
import com.jetbrains.pluginverifier.results.experimental.ExperimentalFieldUsage
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.problems.*
import com.jetbrains.pluginverifier.results.reference.SymbolicReference
import com.jetbrains.pluginverifier.verifiers.*
import com.jetbrains.pluginverifier.verifiers.logic.hierarchy.ClassHierarchyBuilder
import com.jetbrains.pluginverifier.verifiers.resolution.FieldResolution
import com.jetbrains.pluginverifier.verifiers.resolution.FieldResolutionResult
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodNode

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

  private val instruction = Instruction.fromOpcode(instr.opcode) ?: throw IllegalArgumentException()

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
    if (found.fieldNode.isStatic()) {
      val fieldDeclaration = createFieldLocation(found.definingClass, found.fieldNode)
      ctx.registerProblem(InstanceAccessOfStaticFieldProblem(fieldDeclaration, getFromMethod(), Instruction.PUT_FIELD))
    }

    /*
    Otherwise, if the field is final, it must be declared in the current class, and the instruction must occur in
     an instance initialization method (<init>) of the current class. Otherwise, an IllegalAccessError is thrown.
    */

    /*
    This check is according to the JVM 8 spec, but Kotlin and others violate it (Java 8 doesn't complain too)
    if (!(StringUtil.equals(location.getClassNode().name, verifiedClass.name) && "<init>".equals(verifierMethod.name))) {
    */
    if (found.fieldNode.isFinal()) {
      if (found.definingClass.name != verifiableClass.name) {
        val fieldDeclaration = createFieldLocation(found.definingClass, found.fieldNode)
        val accessor = getFromMethod()
        ctx.registerProblem(ChangeFinalFieldProblem(fieldDeclaration, accessor, Instruction.PUT_FIELD))
      }
    }
  }

  fun processGetField() {
    val found = resolveField() ?: return

    //Otherwise, if the resolved field is a static field, getfield throws an IncompatibleClassChangeError.
    if (found.fieldNode.isStatic()) {
      val fieldDeclaration = createFieldLocation(found.definingClass, found.fieldNode)
      ctx.registerProblem(InstanceAccessOfStaticFieldProblem(fieldDeclaration, getFromMethod(), Instruction.GET_FIELD))
    }
  }

  fun processPutStatic() {
    val found = resolveField() ?: return

    //Otherwise, if the resolved field is not a static (class) field or an interface field, putstatic throws an IncompatibleClassChangeError.
    if (!found.fieldNode.isStatic()) {
      val fieldDeclaration = createFieldLocation(found.definingClass, found.fieldNode)
      val methodLocation = getFromMethod()
      ctx.registerProblem(StaticAccessOfInstanceFieldProblem(fieldDeclaration, methodLocation, Instruction.PUT_STATIC))
    }

    /*
    Otherwise, if the field is final, it must be declared in the current class, and the instruction
    must occur in the <clinit> method of the current class. Otherwise, an IllegalAccessError is thrown.

    if (!(StringUtil.equals(location.getClassNode().name, verifiedClass.name) && "<clinit>".equals(verifierMethod.name))) {
    */
    if (found.fieldNode.isFinal()) {
      if (found.definingClass.name != verifiableClass.name) {
        val fieldDeclaration = createFieldLocation(found.definingClass, found.fieldNode)
        val accessor = getFromMethod()
        ctx.registerProblem(ChangeFinalFieldProblem(fieldDeclaration, accessor, Instruction.PUT_STATIC))
      }
    }

  }

  fun processGetStatic() {
    val found = resolveField() ?: return

    //Otherwise, if the resolved field is not a static (class) field or an interface field, getstatic throws an IncompatibleClassChangeError.
    if (!found.fieldNode.isStatic()) {
      val fieldDeclaration = createFieldLocation(found.definingClass, found.fieldNode)
      val methodLocation = getFromMethod()
      ctx.registerProblem(StaticAccessOfInstanceFieldProblem(fieldDeclaration, methodLocation, Instruction.GET_STATIC))
    }
  }


  private fun checkFieldIsAccessible(location: FieldResolutionResult.Found) {
    val definingClass = location.definingClass
    val fieldNode = location.fieldNode

    var accessProblem: AccessType? = null

    when {
      fieldNode.isPrivate() ->
        if (verifiableClass.name != definingClass.name) {
          //accessing to the private field of the other class
          accessProblem = AccessType.PRIVATE
        }
      fieldNode.isProtected() -> {
        if (!haveTheSamePackage(verifiableClass, definingClass)) {
          if (!ctx.isSubclassOf(verifiableClass, definingClass)) {
            accessProblem = AccessType.PROTECTED
          }
        }
      }
      fieldNode.isDefaultAccess() ->
        if (!haveTheSamePackage(verifiableClass, definingClass)) {
          accessProblem = AccessType.PACKAGE_PRIVATE
        }
    }

    if (accessProblem != null) {
      val fieldDeclaration = createFieldLocation(location.definingClass, location.fieldNode)
      val fieldBytecodeReference = SymbolicReference.fieldOf(fieldOwner, fieldName, fieldDescriptor)
      ctx.registerProblem(IllegalFieldAccessProblem(fieldBytecodeReference, fieldDeclaration, getFromMethod(), instruction, accessProblem))
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
  private fun resolveField(): FieldResolutionResult.Found? {
    if (fieldOwner.startsWith("[")) {
      //check that the array type exists
      val arrayType = fieldOwner.extractClassNameFromDescr()
      if (arrayType != null) {
        ctx.resolveClassOrProblem(arrayType, verifiableClass) { getFromMethod() }
      }
      return null
    }

    val ownerNode = ctx.resolveClassOrProblem(fieldOwner, verifiableClass) { getFromMethod() } ?: return null

    val lookupResult = FieldResolution(fieldName, fieldDescriptor, ctx.clsResolver, ctx).resolveField(ownerNode)
    return when (lookupResult) {
      FieldResolutionResult.Abort -> null
      FieldResolutionResult.NotFound -> {
        registerFieldNotFoundProblem(ownerNode)
        null
      }
      is FieldResolutionResult.Found -> {
        checkFieldIsAccessible(lookupResult)
        checkFieldIsUnstable(lookupResult)
        lookupResult
      }
    }
  }

  private fun registerFieldNotFoundProblem(ownerNode: ClassNode) {
    val fieldReference = SymbolicReference.fieldOf(fieldOwner, fieldName, fieldDescriptor)
    val fieldOwnerHierarchy = ClassHierarchyBuilder(ctx).buildClassHierarchy(ownerNode)
    ctx.registerProblem(FieldNotFoundProblem(
        fieldReference,
        getFromMethod(),
        fieldOwnerHierarchy,
        instruction
    ))
  }

  private fun checkFieldIsUnstable(resolvedField: FieldResolutionResult.Found) {
    with(resolvedField) {
      val fieldDeprecated = fieldNode.getDeprecationInfo()
      if (fieldDeprecated != null) {
        ctx.registerDeprecatedUsage(DeprecatedFieldUsage(createFieldLocation(definingClass, fieldNode), getFromMethod(), fieldDeprecated))
      }
      if (fieldNode.isExperimentalApi()) {
        ctx.registerExperimentalApiUsage(ExperimentalFieldUsage(createFieldLocation(definingClass, fieldNode), getFromMethod()))
      }
    }
  }


  fun getFromMethod() = createMethodLocation(verifiableClass, verifiableMethod)

}

