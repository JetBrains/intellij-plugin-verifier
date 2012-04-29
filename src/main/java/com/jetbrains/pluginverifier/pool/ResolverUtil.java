package com.jetbrains.pluginverifier.pool;

import com.jetbrains.pluginverifier.resolvers.Resolver;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Dennis.Ushakov
 */
public class ResolverUtil {
  private final static MethodNode ARRAY_METHOD_NODE = new MethodNode();

  public static MethodNode findMethod(final Resolver resolver, final String className, final String methodName, final String methodDesc) {
    if (className.startsWith("[")) {
      // so a receiver is an array, just assume it does exist =)
      return ARRAY_METHOD_NODE;
    }

    final ClassNode clazz = resolver.findClass(className);
    if (clazz != null) {
      for (Object o : clazz.methods) {
        final MethodNode method = (MethodNode)o;
        if (methodName.equals(method.name) && methodDesc.equals(method.desc)) {
          return method;
        }
      }

      if (clazz.superName != null) {
        final MethodNode method = findMethod(resolver, clazz.superName, methodName, methodDesc);
        if (method != null)
          return method;
      }

      for (Object anInterface : clazz.interfaces) {
        final MethodNode method = findMethod(resolver, (String)anInterface, methodName, methodDesc);
        if (method != null)
          return method;
      }
    }

    return null;
  }
}
