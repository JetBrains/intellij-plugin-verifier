package com.jetbrains.pluginverifier.verifiers.field;

import com.jetbrains.pluginverifier.ErrorRegister;
import com.jetbrains.pluginverifier.pool.Resolver;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * @author Dennis.Ushakov
 */
public class FieldTypeVerifier implements FieldVerifier {
  public void verify(final ClassNode clazz, final FieldNode field, final Resolver resolver, final ErrorRegister register) {
    final String className = field.desc;
    if(className == null || VerifierUtil.isNativeType(className) ||
        VerifierUtil.classExists(resolver, className)) return;
    register.registerError(clazz.name, "field "  + field.name + ", type " + className + " not found");
  }
}
