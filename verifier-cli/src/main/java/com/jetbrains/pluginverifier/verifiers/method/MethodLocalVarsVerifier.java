package com.jetbrains.pluginverifier.verifiers.method;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.verifiers.VContext;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

/**
 * @author Sergey Patrikeev
 */
public class MethodLocalVarsVerifier implements MethodVerifier {
  @SuppressWarnings("unchecked")
  @Override
  public void verify(ClassNode clazz, MethodNode method, Resolver resolver, VContext ctx) {
    List<LocalVariableNode> localVariables = (List<LocalVariableNode>) method.localVariables;
    if (localVariables != null) {
      for (LocalVariableNode variable : localVariables) {
        String descr = VerifierUtil.extractClassNameFromDescr(variable.desc);
        if (descr == null) continue;
        if (!VerifierUtil.classExistsOrExternal(ctx, clazz, resolver, descr)) {
          ctx.registerProblem(new ClassNotFoundProblem(descr), ProblemLocation.fromMethod(clazz.name, method));
        }
      }
    }

  }
}
