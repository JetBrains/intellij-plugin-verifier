package com.jetbrains.pluginverifier.verifiers.field;

import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.verifiers.VContext;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * @author Dennis.Ushakov
 */
public interface FieldVerifier {
  void verify(ClassNode clazz, FieldNode field, Resolver resolver, VContext ctx);
}
