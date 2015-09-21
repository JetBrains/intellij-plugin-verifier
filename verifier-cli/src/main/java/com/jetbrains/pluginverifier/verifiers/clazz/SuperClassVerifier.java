package com.jetbrains.pluginverifier.verifiers.clazz;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.problems.ProblemLocation;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.tree.ClassNode;

/**
 * Check that superclass exists.
 *
 * @author Dennis.Ushakov
 */
public class SuperClassVerifier implements ClassVerifier {
  public void verify(final ClassNode clazz, final Resolver resolver, final VerificationContext ctx) {
    final String superClassName = clazz.superName;
    if(!VerifierUtil.classExists(ctx.getOptions(), resolver, superClassName, false)) {
      ctx.registerProblem(new ClassNotFoundProblem(superClassName), new ProblemLocation(clazz.name));
    }
  }
}
