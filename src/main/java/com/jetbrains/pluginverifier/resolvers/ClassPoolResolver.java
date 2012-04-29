package com.jetbrains.pluginverifier.resolvers;

import com.jetbrains.pluginverifier.pool.ClassPool;
import org.objectweb.asm.tree.ClassNode;

public class ClassPoolResolver implements Resolver {
  private final ClassPool myPool;

  public ClassPoolResolver(ClassPool pool) {
    myPool = pool;
  }

  @Override
  public ClassNode findClass(final String className) {
    return myPool.getClassNode(className);
  }

  @Override
  public String getClassLocationMoniker(final String className) {
    return myPool.getClassLocationMoniker(className);
  }
}
