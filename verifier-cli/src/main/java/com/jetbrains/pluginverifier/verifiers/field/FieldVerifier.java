package com.jetbrains.pluginverifier.verifiers.field;

import com.jetbrains.pluginverifier.resolvers.Resolver;
import com.jetbrains.pluginverifier.util.Consumer;
import com.jetbrains.pluginverifier.problems.Problem;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * @author Dennis.Ushakov
 */
public interface FieldVerifier {
  void verify(ClassNode clazz, FieldNode field, Resolver resolver, Consumer<Problem> register);
}
