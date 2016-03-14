package com.intellij.structure.impl.resolvers;

import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Sergey Evdokimov
 */
public class CacheResolver extends Resolver {

  private final Resolver myDelegate;

  //TODO: WeakHashMap
  private final Map<String, ClassNode> myCache = new HashMap<String, ClassNode>();

  public CacheResolver(@NotNull Resolver delegate) {
    myDelegate = delegate;
  }

  @Override
  @Nullable
  public ClassNode findClass(@NotNull String className) {
    ClassNode res = myCache.get(className);
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

  @Override
  public String toString() {
    return myDelegate.toString();
  }

  @NotNull
  @Override
  public Set<String> getAllClasses() {
    return myDelegate.getAllClasses();
  }

  @Override
  public boolean isEmpty() {
    return myDelegate.isEmpty();
  }
}
