package com.jetbrains.pluginverifier.verifiers.clazz;

import com.jetbrains.pluginverifier.ErrorRegister;
import com.jetbrains.pluginverifier.pool.Resolver;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author Dennis.Ushakov
 */
public interface ClassVerifier {
  void verify(ClassNode clazz, Resolver resolver, ErrorRegister register);
}
