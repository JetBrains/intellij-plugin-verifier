package com.jetbrains.pluginverifier.verifiers.clazz;

import com.jetbrains.pluginverifier.ErrorRegister;
import com.jetbrains.pluginverifier.pool.Resolver;
import com.jetbrains.pluginverifier.verifiers.util.VerifierUtil;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author Dennis.Ushakov
 */
public class InterfacesVerifier implements ClassVerifier {
  public void verify(final ClassNode clazz, final Resolver resolver, final ErrorRegister register) {
    for (Object o : clazz.interfaces) {
      final String iface = (String)o;
      if(!VerifierUtil.classExists(resolver, iface, true)) {
        register.registerError(clazz.name, "implemented interface " + iface + " not found");
        return;
      }
    }
  }
}
