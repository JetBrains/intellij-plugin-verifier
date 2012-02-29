package com.jetbrains.pluginverifier.pool;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Dennis.Ushakov
 */
public class Resolver {
  private final static MethodNode ARRAY_METHOD_NODE = new MethodNode();

  private final ClassPool[] myPools;
  private final String myName;

  public Resolver(final String name, final ClassPool... pools) {
    myName = name;
    myPools = pools;
  }

  private static ClassPool getClassPool(final ClassPool source, final String className) {
    if (source instanceof ContainerClassPool) {
      for (ClassPool pool : ((ContainerClassPool) source).getPools()) {
        ClassPool result = getClassPool(pool, className);
        if (result != null)
          return result;
      }

      return null;
    }

    return source.getAllClasses().contains(className) ? source : null;
  }

  public ClassPool getClassPool(final String className) {
    for (ClassPool pool : myPools) {
      ClassPool result = getClassPool(pool, className);
      if (result != null)
        return result;
    }

    return null;
  }

  public ClassNode findClass(final String className) {
    for (final ClassPool pool : myPools) {
      final ClassNode node = pool.getClassNode(className);
      if (node != null) return node;
    }
    return null;
  }

  public MethodNode findMethod(final String className, final String methodName, final String methodDesc) {
    if (className.startsWith("[")) {
      // so a receiver is an array, just assume it does exist =)
      return ARRAY_METHOD_NODE;
    }

    final ClassNode clazz = findClass(className);
    if (clazz != null) {
      for (Object o : clazz.methods) {
        final MethodNode method = (MethodNode)o;
        if (methodName.equals(method.name) && methodDesc.equals(method.desc)) {
          return method;
        }
      }

      if (clazz.superName != null) {
        final MethodNode method = findMethod(clazz.superName, methodName, methodDesc);
        if (method != null) return method;
      }

      for (Object anInterface : clazz.interfaces) {
        final MethodNode method = findMethod((String)anInterface, methodName, methodDesc);
        if (method != null) return method;
      }
    }

    return null;
  }

  public String getName() {
    return myName;
  }
}
