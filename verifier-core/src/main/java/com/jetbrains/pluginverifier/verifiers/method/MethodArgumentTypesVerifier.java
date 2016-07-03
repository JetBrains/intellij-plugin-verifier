package com.jetbrains.pluginverifier.verifiers.method;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.utils.VerifierUtil;
import com.jetbrains.pluginverifier.verifiers.VContext;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Sergey Patrikeev
 */
public class MethodArgumentTypesVerifier implements MethodVerifier {
  @Override
  public void verify(ClassNode clazz, MethodNode method, Resolver resolver, VContext ctx) {
    Type methodType = Type.getType(method.desc);
    Type[] argumentTypes = methodType.getArgumentTypes();
    for (Type type : argumentTypes) {
      String argDescr = VerifierUtil.extractClassNameFromDescr(type.getDescriptor());
      if (argDescr == null) continue;
      if (!VerifierUtil.classExistsOrExternal(ctx, resolver, argDescr)) {
        ctx.registerProblem(new ClassNotFoundProblem(argDescr), ProblemLocation.fromMethod(clazz.name, method));
      }
    }


  }
}
