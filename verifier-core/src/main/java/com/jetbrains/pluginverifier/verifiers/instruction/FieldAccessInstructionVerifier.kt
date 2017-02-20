package com.jetbrains.pluginverifier.verifiers.instruction

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.problems.*
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.jetbrains.intellij.plugins.internal.asm.Opcodes
import org.jetbrains.intellij.plugins.internal.asm.tree.*

/**
 * Verifies GETSTATIC, PUTSTATIC, GETFIELD and PUTFIELD instructions
 *
 * @author Sergey Patrikeev
 */
class FieldAccessInstructionVerifier : InstructionVerifier {

  override fun verify(clazz: ClassNode, method: MethodNode, instr: AbstractInsnNode, resolver: Resolver, ctx: VContext) {
    if (instr is FieldInsnNode) {
      FieldsImplementation(clazz, method, instr, resolver, ctx).verify()
    }
  }

}

private class FieldsImplementation(val verifiableClass: ClassNode,
                                   val verifiableMethod: MethodNode,
                                   val instr: FieldInsnNode,
                                   val resolver: Resolver,
                                   val ctx: VContext,
                                   val fieldOwner: String = instr.owner,
                                   val fieldName: String = instr.name,
                                   val fieldDescriptor: String = instr.desc) {
  fun verify() {

    when (instr.opcode) {
      Opcodes.PUTFIELD -> processPutField()
      Opcodes.GETFIELD -> processGetField()
      Opcodes.PUTSTATIC -> processPutStatic()
      Opcodes.GETSTATIC -> processGetStatic()
      else -> throw RuntimeException("Unknown opcode ${instr.opcode} of instruction: $instr")
    }
  }

  fun processPutField() {
    val found = resolveField() ?: return

    /*
    Otherwise, if the resolved field is a static field, putfield throws an IncompatibleClassChangeError.
     */
    if (VerifierUtil.isStatic(found.fieldNode)) {
      ctx.registerProblem(InstanceAccessOfStaticFieldProblem(found.definingClass.name, found.fieldNode.name, found.fieldNode.desc), getFromMethod())
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
        ctx.registerProblem(ChangeFinalFieldProblem(found.definingClass.name, fieldName, fieldDescriptor), getFromMethod())
      }
    }
  }

  fun processGetField() {
    val found = resolveField() ?: return

    //Otherwise, if the resolved field is a static field, getfield throws an IncompatibleClassChangeError.
    if (VerifierUtil.isStatic(found.fieldNode)) {
      ctx.registerProblem(InstanceAccessOfStaticFieldProblem(found.definingClass.name, found.fieldNode.name, found.fieldNode.desc), getFromMethod())
    }
  }

  fun processPutStatic() {
    val found = resolveField() ?: return

    //Otherwise, if the resolved field is not a static (class) field or an interface field, putstatic throws an IncompatibleClassChangeError.
    if (!VerifierUtil.isStatic(found.fieldNode)) {
      ctx.registerProblem(StaticAccessOfInstanceFieldProblem(found.definingClass.name, found.fieldNode.name, found.fieldNode.desc), getFromMethod())
    }

    /*
    Otherwise, if the field is final, it must be declared in the current class, and the instruction
    must occur in the <clinit> method of the current class. Otherwise, an IllegalAccessError is thrown.

    if (!(StringUtil.equals(location.getClassNode().name, verifiedClass.name) && "<clinit>".equals(verifierMethod.name))) {
    */
    if (VerifierUtil.isFinal(found.fieldNode)) {
      if (found.definingClass.name != verifiableClass.name) {
        ctx.registerProblem(ChangeFinalFieldProblem(found.definingClass.name, fieldName, fieldDescriptor), getFromMethod())
      }
    }

  }

  fun processGetStatic() {
    val found = resolveField() ?: return

    //Otherwise, if the resolved field is not a static (class) field or an interface field, getstatic throws an IncompatibleClassChangeError.
    if (!VerifierUtil.isStatic(found.fieldNode)) {
      ctx.registerProblem(StaticAccessOfInstanceFieldProblem(found.definingClass.name, found.fieldNode.name, found.fieldNode.desc), getFromMethod())
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
          if (!VerifierUtil.isSubclassOf(verifiableClass, definingClass, resolver, ctx)) {
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
      ctx.registerProblem(IllegalFieldAccessProblem(definingClass.name, fieldNode.name, fieldNode.desc, accessProblem), getFromMethod())
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
        VerifierUtil.checkClassExistsOrExternal(resolver, arrayType, ctx, { getFromMethod() })
      }
      return null
    }

    val resolveClass = VerifierUtil.resolveClassOrProblem(resolver, fieldOwner, verifiableClass, ctx, { getFromMethod() }) ?: return null

    val (fail, resolvedField) = resolveFieldSteps(resolveClass)
    if (fail) {
      return null
    }
    if (resolvedField == null) {
      ctx.registerProblem(FieldNotFoundProblem(fieldOwner, fieldName, fieldDescriptor), getFromMethod())
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
      val resolvedIntf = VerifierUtil.resolveClassOrProblem(resolver, anInterface, currentClass, ctx, { ctx.fromClass(currentClass) }) ?: return FAILED_LOOKUP

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
      val resolvedSuper = VerifierUtil.resolveClassOrProblem(resolver, superName, currentClass, ctx, { ctx.fromClass(currentClass) }) ?: return FAILED_LOOKUP
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

