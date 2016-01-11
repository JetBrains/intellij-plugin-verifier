package com.intellij.structure.impl.resolvers;

import com.intellij.structure.bytecode.ClassFile;
import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Sergey Evdokimov
 */
public class CacheResolver implements Resolver {

  private final Resolver myDelegate;

  private final Map<String, ClassFile> myCache = new HashMap<String, ClassFile>();

  private CacheResolver(Resolver delegate) {
    myDelegate = delegate;
  }

  public static CacheResolver createCacheResolver(Resolver delegate) {
    return new CacheResolver(delegate);
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
  public Resolver getClassLocation(@NotNull String className) {
    return myDelegate.getClassLocation(className);
  }

  @Nullable
  @Override
  public String getMoniker() {
    return myDelegate.getMoniker();
  }
}
