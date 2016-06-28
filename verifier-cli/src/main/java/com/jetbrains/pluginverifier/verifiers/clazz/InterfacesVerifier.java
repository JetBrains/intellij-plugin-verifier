package com.jetbrains.pluginverifier.verifiers.clazz;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem;
import com.jetbrains.pluginverifier.problems.IncompatibleClassChangeProblem;
import com.jetbrains.pluginverifier.verifiers.VContext;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.tree.ClassNode;

/**
 * Check that all explicitly defined interfaces exists.
 *
 * @author Dennis.Ushakov
 */
public class InterfacesVerifier implements ClassVerifier {
  public void verify(final ClassNode clazz, final Resolver resolver, final VContext ctx) {
    for (Object o : clazz.interfaces) {
      final String iface = (String)o;
      ClassNode node = VerifierUtil.findClass(resolver, iface, ctx);
      if (node == null) {
        if (!ctx.getVerifierOptions().isExternalClass(iface)) {
          ctx.registerProblem(new ClassNotFoundProblem(iface), ProblemLocation.fromClass(clazz.name));
        }
        continue;
      }
      if (!VerifierUtil.isInterface(node)) {
        ctx.registerProblem(new IncompatibleClassChangeProblem(iface, IncompatibleClassChangeProblem.Change.INTERFACE_TO_CLASS), ProblemLocation.fromClass(clazz.name));
      }
    }
  }
}
