package com.jetbrains.pluginverifier.verifiers.method;

import com.jetbrains.pluginverifier.resolvers.Resolver;
import com.jetbrains.pluginverifier.util.Consumer;
import com.jetbrains.pluginverifier.problems.Problem;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Dennis.Ushakov
 */
public interface MethodVerifier {
  void verify(ClassNode clazz, MethodNode method, Resolver resolver, Consumer<Problem> register);
}
