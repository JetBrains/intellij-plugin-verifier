package com.jetbrains.pluginverifier.verifiers.clazz;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.problems.IncompatibleClassChangeProblem;
import com.jetbrains.pluginverifier.verifiers.VerificationContext;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.tree.ClassNode;

/**
 * Check that superclass exists.
 *
 * @author Dennis.Ushakov
 */
public class SuperClassVerifier implements ClassVerifier {
  public void verify(final ClassNode clazz, final Resolver resolver, final VerificationContext ctx) {
    final String superClassName = clazz.superName == null ? "java/lang/Object" : clazz.superName;
    ClassNode aClass = VerifierUtil.findClass(resolver, superClassName, ctx);
    if (aClass == null) {
      if (!ctx.getVerifierOptions().isExternalClass(superClassName)) {
        ctx.registerProblem(new ClassNotFoundProblem(superClassName), ProblemLocation.fromClass(clazz.name));
      }
      return;
    }
    if (VerifierUtil.isInterface(aClass)) {
      ctx.registerProblem(new IncompatibleClassChangeProblem(superClassName, IncompatibleClassChangeProblem.Change.CLASS_TO_INTERFACE), ProblemLocation.fromClass(clazz.name));
    }
  }
}
