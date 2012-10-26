package com.jetbrains.pluginverifier.verifiers.clazz;

import com.jetbrains.pluginverifier.pool.ResolverUtil;
import com.jetbrains.pluginverifier.problems.SuperClassNotFoundProblem;
import com.jetbrains.pluginverifier.problems.SuperMethodNotFoundProblem;
import com.jetbrains.pluginverifier.resolvers.Resolver;
import com.jetbrains.pluginverifier.util.Consumer;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Dennis.Ushakov
 */
public class SuperClassVerifier implements ClassVerifier {
  public void verify(final ClassNode clazz, final Resolver resolver, final Consumer<Problem> errorHandler) {
    final String className = clazz.superName;
    if(!VerifierUtil.classExists(resolver, className, false))  {
      errorHandler.consume(new SuperClassNotFoundProblem(clazz.name, className));
      return;
    }

    if ((clazz.access & Opcodes.ACC_ABSTRACT) != 0)
      return;

    final ClassNode superClass = resolver.findClass(className);
    for (Object o : superClass.methods) {
      final MethodNode method = (MethodNode)o;
      if (!VerifierUtil.isAbstract(method)) {
        final MethodNode impl = ResolverUtil.findMethod(resolver, clazz.name, method.name, method.desc);
        if (impl == null) {
          errorHandler.consume(new SuperMethodNotFoundProblem(clazz.name, method.name + method.desc));
        }
      }
    }
  }
}
