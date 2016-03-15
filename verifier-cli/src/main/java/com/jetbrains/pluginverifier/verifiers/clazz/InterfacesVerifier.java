package com.jetbrains.pluginverifier.verifiers.clazz;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.VerificationContext;
import com.jetbrains.pluginverifier.error.VerificationError;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.results.ProblemLocation;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.tree.ClassNode;

/**
 * Check that all explicitly defined interfaces exists.
 *
 * @author Dennis.Ushakov
 */
public class InterfacesVerifier implements ClassVerifier {
  public void verify(final ClassNode clazz, final Resolver resolver, final VerificationContext ctx) throws VerificationError {
    for (Object o : clazz.interfaces) {
      final String iface = (String)o;
      if (!VerifierUtil.classExists(ctx.getVerifierOptions(), resolver, iface, true)) {
        ctx.registerProblem(new ClassNotFoundProblem(iface), ProblemLocation.fromClass(clazz.name));
        return;
      }
    }
  }
}
