package com.jetbrains.pluginverifier.verifiers.instruction

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.problems.FieldNotFoundProblem
import com.jetbrains.pluginverifier.problems.MethodNotFoundProblem
import com.jetbrains.pluginverifier.utils.LocationUtils
import com.jetbrains.pluginverifier.utils.ResolverUtil
import com.jetbrains.pluginverifier.utils.VerifierUtil
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodNode
import java.util.*

/**
 * @author Sergey Patrikeev
 */
class InvokeDynamicVerifier : InstructionVerifier {

  override fun verify(clazz: ClassNode, method: MethodNode, instr: AbstractInsnNode, resolver: Resolver, ctx: VContext) {
    if (instr !is InvokeDynamicInsnNode) {
      return
    }

    val bsmArgs = instr.bsmArgs
    if (bsmArgs != null) {
      for (arg in bsmArgs) {
        if (arg is Handle) {

          val owner = arg.owner
          val aClass = VerifierUtil.findClass(resolver, owner, ctx)
          if (aClass == null) {
            if (!ctx.verifierOptions.isExternalClass(owner)) {
              ctx.registerProblem(ClassNotFoundProblem(owner), ProblemLocation.fromMethod(clazz.name, method))
            }
            continue
          }

          if (METHOD_TAGS.contains(arg.tag)) {
            val location = ResolverUtil.findMethod(resolver, aClass, arg.name, arg.desc, ctx)
            if (location == null) {
              val methodLocation = LocationUtils.getMethodLocation(aClass, arg.name, arg.desc)
              ctx.registerProblem(MethodNotFoundProblem(methodLocation), ProblemLocation.fromMethod(clazz.name, method))
            }
          } else if (FIELD_TAGS.contains(arg.tag)) {
            val location = ResolverUtil.findField(resolver, aClass, arg.name, arg.desc, ctx)
            if (location == null) {
              val fieldLocation = LocationUtils.getFieldLocation(aClass, arg.name, arg.desc)
              ctx.registerProblem(FieldNotFoundProblem(fieldLocation), ProblemLocation.fromMethod(clazz.name, method))
            }
            //TODO; write a test for the case
          }

        }
      }
    }


  }

  companion object {

    private val FIELD_TAGS = Arrays.asList(Opcodes.H_GETFIELD, Opcodes.H_GETSTATIC, Opcodes.H_PUTFIELD, Opcodes.H_PUTSTATIC)
    private val METHOD_TAGS = Arrays.asList(Opcodes.H_INVOKEVIRTUAL, Opcodes.H_INVOKESTATIC, Opcodes.H_INVOKESPECIAL, Opcodes.H_NEWINVOKESPECIAL, Opcodes.H_INVOKEINTERFACE)
  }
}
