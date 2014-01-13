package com.jetbrains.pluginverifier.resolvers;

import org.objectweb.asm.tree.ClassNode;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Sergey Evdokimov
 */
public class CacheResolver implements Resolver {

  private final Resolver delegate;

  private final Map<String, ClassNode> cache = new HashMap<String, ClassNode>();

  public CacheResolver(Resolver delegate) {
    this.delegate = delegate;
  }

  @Override
  public ClassNode findClass(String className) {
    ClassNode res = cache.get(className);
    if (res == null) {
      if (!cache.containsKey(className)) {
        res = delegate.findClass(className);
        cache.put(className, res);
      }
    }
    return res;
  }

  @Override
  public String getClassLocationMoniker(String className) {
    return delegate.getClassLocationMoniker(className);
  }

}
