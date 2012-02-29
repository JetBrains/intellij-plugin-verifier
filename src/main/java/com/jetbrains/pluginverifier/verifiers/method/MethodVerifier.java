package com.jetbrains.pluginverifier.verifiers.method;

import com.jetbrains.pluginverifier.ErrorRegister;
import com.jetbrains.pluginverifier.pool.Resolver;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Dennis.Ushakov
 */
public interface MethodVerifier {
  void verify(ClassNode clazz, MethodNode method, Resolver resolver, ErrorRegister register);
}
