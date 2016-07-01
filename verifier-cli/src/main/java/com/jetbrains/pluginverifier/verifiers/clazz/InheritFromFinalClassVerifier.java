package com.jetbrains.pluginverifier.verifiers.clazz;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.problems.InheritFromFinalClassProblem;
import com.jetbrains.pluginverifier.verifiers.VerificationContext;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.tree.ClassNode;

public class InheritFromFinalClassVerifier implements ClassVerifier {
  public void verify(final ClassNode clazz, final Resolver resolver, final VerificationContext ctx) {
    final String superClassName = clazz.superName == null ? "java/lang/Object" : clazz.superName;
    ClassNode supClass = VerifierUtil.findClass(resolver, superClassName, ctx);
    if (supClass == null) {
      if (!ctx.getVerifierOptions().isExternalClass(superClassName)) {
        ctx.registerProblem(new ClassNotFoundProblem(superClassName), ProblemLocation.fromClass(clazz.name));
      }
      return;
    }
    if (VerifierUtil.isFinal(supClass)) {
      ctx.registerProblem(new InheritFromFinalClassProblem(supClass.name), ProblemLocation.fromClass(clazz.name));
    }
  }
}
