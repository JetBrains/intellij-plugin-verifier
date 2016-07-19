package com.jetbrains.pluginverifier.verifiers.instruction;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.api.VContext;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.problems.FieldNotFoundProblem;
import com.jetbrains.pluginverifier.problems.MethodNotFoundProblem;
import com.jetbrains.pluginverifier.utils.LocationUtils;
import com.jetbrains.pluginverifier.utils.ResolverUtil;
import com.jetbrains.pluginverifier.utils.VerifierUtil;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;
import java.util.List;

/**
 * @author Sergey Patrikeev
 */
public class InvokeDynamicVerifier implements InstructionVerifier {

  private static final List<Integer> FIELD_TAGS = Arrays.asList(Opcodes.H_GETFIELD, Opcodes.H_GETSTATIC, Opcodes.H_PUTFIELD, Opcodes.H_PUTSTATIC);
  private static final List<Integer> METHOD_TAGS = Arrays.asList(Opcodes.H_INVOKEVIRTUAL, Opcodes.H_INVOKESTATIC, Opcodes.H_INVOKESPECIAL, Opcodes.H_NEWINVOKESPECIAL, Opcodes.H_INVOKEINTERFACE);

  @Override
  public void verify(ClassNode clazz, MethodNode method, AbstractInsnNode instr, Resolver resolver, VContext ctx) {
    if (!(instr instanceof InvokeDynamicInsnNode)) {
      return;
    }
    InvokeDynamicInsnNode dynamicInsnNode = (InvokeDynamicInsnNode) instr;

    Object[] bsmArgs = dynamicInsnNode.bsmArgs;
    if (bsmArgs != null) {
      for (Object arg : bsmArgs) {
        if (arg instanceof Handle) {

          Handle handle = (Handle) arg;

          String owner = handle.getOwner();
          ClassNode aClass = VerifierUtil.findClass(resolver, owner, ctx);
          if (aClass == null) {
            if (!ctx.getVerifierOptions().isExternalClass(owner)) {
              ctx.registerProblem(new ClassNotFoundProblem(owner), ProblemLocation.fromMethod(clazz.name, method));
            }
            continue;
          }

          if (METHOD_TAGS.contains(handle.getTag())) {
            ResolverUtil.MethodLocation location = ResolverUtil.findMethod(resolver, aClass, handle.getName(), handle.getDesc(), ctx);
            if (location == null) {
              String methodLocation = LocationUtils.INSTANCE.getMethodLocation(aClass, handle.getName(), handle.getDesc());
              ctx.registerProblem(new MethodNotFoundProblem(methodLocation), ProblemLocation.fromMethod(clazz.name, method));
            }
          } else if (FIELD_TAGS.contains(handle.getTag())) {
            ResolverUtil.FieldLocation location = ResolverUtil.findField(resolver, aClass, handle.getName(), handle.getDesc(), ctx);
            if (location == null) {
              String fieldLocation = LocationUtils.INSTANCE.getFieldLocation(aClass, handle.getName(), handle.getDesc());
              ctx.registerProblem(new FieldNotFoundProblem(fieldLocation), ProblemLocation.fromMethod(clazz.name, method));
            }
            //TODO; write a test for the case
          }

        }
      }
    }


  }
}
