package com.intellij.structure.resolvers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.org.objectweb.asm.tree.ClassNode;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Sergey Evdokimov
 */
public class CacheResolver implements Resolver {

  private final Resolver myDelegate;

  private final Map<String, ClassNode> myCache = new HashMap<String, ClassNode>();

  public CacheResolver(Resolver delegate) {
    myDelegate = delegate;
  }

  @Override
  public ClassNode findClass(@NotNull String className) {
    ClassNode res = myCache.get(className);
    if (res == null) {
      res = myDelegate.findClass(className);
      myCache.put(className, res);
    }
    return res;
  }

  @Override
  public String getClassLocationMoniker(@NotNull String className) {
    return myDelegate.getClassLocationMoniker(className);
  }
}
