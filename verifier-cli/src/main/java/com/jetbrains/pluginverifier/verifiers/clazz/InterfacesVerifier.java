package com.jetbrains.pluginverifier.verifiers.clazz;

import com.jetbrains.pluginverifier.problems.InterfaceNotFoundProblem;
import com.jetbrains.pluginverifier.resolvers.Resolver;
import com.jetbrains.pluginverifier.util.Consumer;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author Dennis.Ushakov
 */
public class InterfacesVerifier implements ClassVerifier {
  public void verify(final ClassNode clazz, final Resolver resolver, final Consumer<Problem> errorHandler) {
    for (Object o : clazz.interfaces) {
      final String iface = (String)o;
      if(!VerifierUtil.classExists(resolver, iface, true)) {
        errorHandler.consume(new InterfaceNotFoundProblem(clazz.name, iface));
        return;
      }
    }
  }
}
