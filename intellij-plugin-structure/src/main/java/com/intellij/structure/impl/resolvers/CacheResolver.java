package com.intellij.structure.impl.resolvers;

import com.intellij.structure.bytecode.ClassFile;
import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Sergey Evdokimov
 */
public class CacheResolver implements Resolver {

  private final Resolver myDelegate;

  private final Map<String, ClassFile> myCache = new HashMap<String, ClassFile>();

  public CacheResolver(Resolver delegate) {
    myDelegate = delegate;
  }

  @Override
  public ClassFile findClass(@NotNull String className) {
    ClassFile res = myCache.get(className);
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
