package com.jetbrains.pluginverifier.verifiers.clazz;

import com.jetbrains.pluginverifier.resolvers.Resolver;
import com.jetbrains.pluginverifier.util.Consumer;
import com.jetbrains.pluginverifier.problems.Problem;
import org.objectweb.asm.tree.ClassNode;

public interface ClassVerifier {
  void verify(ClassNode clazz, Resolver resolver, Consumer<Problem> errorHandler);
}
