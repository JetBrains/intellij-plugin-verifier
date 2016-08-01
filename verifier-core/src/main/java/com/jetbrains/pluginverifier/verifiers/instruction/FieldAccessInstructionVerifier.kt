package com.jetbrains.pluginverifier.verifiers.instruction

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.AccessType
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.problems.FieldNotFoundProblem
import com.jetbrains.pluginverifier.problems.IllegalFieldAccessProblem
import com.jetbrains.pluginverifier.problems.fields.ChangeFinalFieldProblem
import com.jetbrains.pluginverifier.problems.statics.InstanceAccessOfStaticFieldProblem
import com.jetbrains.pluginverifier.problems.statics.StaticAccessOfInstanceFieldProblem
import com.jetbrains.pluginverifier.utils.LocationUtils
import com.jetbrains.pluginverifier.utils.ResolverUtil
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodNode

/**
 * @author Sergey Patrikeev
 */
class FieldAccessInstructionVerifier : InstructionVerifier {
  override fun verify(clazz: ClassNode, method: MethodNode, instr: AbstractInsnNode, resolver: Resolver, ctx: VContext) {
    if (instr !is FieldInsnNode) return

    var fieldOwner: String? = instr.owner
    if (fieldOwner!!.startsWith("[")) {
      //this is an array field => assume it does exist
      return
    }
    fieldOwner = VerifierUtil.extractClassNameFromDescr(fieldOwner)
    if (fieldOwner == null) {
      return
    }

    if (ctx.verifierOptions.isExternalClass(fieldOwner)) {
      //assume the external class contains the field
      return
    }
    val ownerNode = VerifierUtil.findClass(resolver, clazz, fieldOwner, ctx)
    if (ownerNode == null) {
      ctx.registerProblem(ClassNotFoundProblem(fieldOwner), ProblemLocation.fromMethod(clazz.name, method))
      return
    }

    val actualLocation = ResolverUtil.findField(resolver, ownerNode, instr.name, instr.desc, ctx)
    if (actualLocation == null) {

      if (VerifierUtil.hasUnresolvedParentClass(fieldOwner, resolver, ctx)) {
        //field owner has some unresolved class => most likely that this class contains(-ed) the sought-for field
        return
      }

      val fieldLocation = LocationUtils.getFieldLocation(ownerNode.name, instr.name, instr.desc)
      ctx.registerProblem(FieldNotFoundProblem(fieldLocation), ProblemLocation.fromMethod(clazz.name, method))
      return
    }

    //check that access permission exists
    checkAccess(actualLocation, ctx, resolver, clazz, method)


    val opcode = instr.opcode

    val field = LocationUtils.getFieldLocation(actualLocation.classNode.name, actualLocation.fieldNode)

    if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) {
      if (!VerifierUtil.isStatic(actualLocation.fieldNode)) { //TODO: "if the resolved field is not a static field or an interface field, getstatic throws an IncompatibleClassChangeError"
        ctx.registerProblem(StaticAccessOfInstanceFieldProblem(field), ProblemLocation.fromMethod(clazz.name, method))
      }
    } else if (opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD) {
      if (VerifierUtil.isStatic(actualLocation.fieldNode)) {
        ctx.registerProblem(InstanceAccessOfStaticFieldProblem(field), ProblemLocation.fromMethod(clazz.name, method))
      }
    }

    checkFinalModifier(opcode, actualLocation, ctx, clazz, method)

  }

  private fun checkFinalModifier(opcode: Int, location: ResolverUtil.FieldLocation, ctx: VContext, verifiedClass: ClassNode, verifierMethod: MethodNode) {
    val field = LocationUtils.getFieldLocation(location.classNode.name, location.fieldNode)

    if (VerifierUtil.isFinal(location.fieldNode)) {
      if (opcode == Opcodes.PUTFIELD) {
        /*
        TODO: this check is according to the JVM 8 spec, but Kotlin and others violate it (Java 8 doesn't complain too)
        if (!(StringUtil.equals(location.getClassNode().name, verifiedClass.name) && "<init>".equals(verifierMethod.name))) {
       */
        if (!location.classNode.name.equals(verifiedClass.name, false)) {
          ctx.registerProblem(ChangeFinalFieldProblem(field), ProblemLocation.fromMethod(verifiedClass.name, verifierMethod))
        }
      }

      if (opcode == Opcodes.PUTSTATIC) {
        //        if (!(StringUtil.equals(location.getClassNode().name, verifiedClass.name) && "<clinit>".equals(verifierMethod.name))) {
        if (!location.classNode.name.equals(verifiedClass.name, false)) {
          ctx.registerProblem(ChangeFinalFieldProblem(field), ProblemLocation.fromMethod(verifiedClass.name, verifierMethod))
        }
      }
    }

  }

  private fun checkAccess(location: ResolverUtil.FieldLocation, ctx: VContext, resolver: Resolver, verifiedClass: ClassNode, verifiedMethod: MethodNode) {
    val actualOwner = location.classNode
    val actualField = location.fieldNode

    var accessProblem: AccessType? = null

    if (VerifierUtil.isPrivate(actualField)) {
      if (!verifiedClass.name.equals(actualOwner.name, false)) {
        //accessing to the private field of the other class
        accessProblem = AccessType.PRIVATE
      }
    } else if (VerifierUtil.isProtected(actualField)) {
      if (!VerifierUtil.isAncestor(verifiedClass, actualOwner, resolver, ctx) && !VerifierUtil.haveTheSamePackage(verifiedClass, actualOwner)) {
        accessProblem = AccessType.PROTECTED
      }
    } else if (VerifierUtil.isDefaultAccess(actualField)) {
      if (!VerifierUtil.haveTheSamePackage(verifiedClass, actualOwner)) {
        accessProblem = AccessType.PACKAGE_PRIVATE
      }
    }

    if (accessProblem != null) {
      val problem = IllegalFieldAccessProblem(LocationUtils.getFieldLocation(actualOwner.name, actualField), accessProblem)
      ctx.registerProblem(problem, ProblemLocation.fromMethod(verifiedClass.name, verifiedMethod))
    }


  }
}
