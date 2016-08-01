package com.jetbrains.pluginverifier.verifiers.method;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.api.VContext;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.utils.VerifierUtil;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

/**
 * @author Sergey Patrikeev
 */
public class MethodThrowsVerifier implements MethodVerifier {
  @SuppressWarnings("unchecked")
  @Override
  public void verify(ClassNode clazz, MethodNode method, Resolver resolver, VContext ctx) {
    List<String> exceptions = (List<String>) method.exceptions;
    for (String exception : exceptions) {
      String descr = VerifierUtil.extractClassNameFromDescr(exception);
      if (descr == null) continue;
      if (!VerifierUtil.classExistsOrExternal(ctx, resolver, descr)) {
        ctx.registerProblem(new ClassNotFoundProblem(descr), ProblemLocation.fromMethod(clazz.name, method));
      }
    }
  }
}
