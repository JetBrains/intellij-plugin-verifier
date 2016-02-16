package com.intellij.structure.impl.resolvers;

import com.intellij.structure.bytecode.ClassFile;
import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Sergey Evdokimov
 */
public class CacheResolver extends Resolver {

  private final Resolver myDelegate;

  private final Map<String, ClassFile> myCache = new HashMap<String, ClassFile>();

  public CacheResolver(@NotNull Resolver delegate) {
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
  public Resolver getClassLocation(@NotNull String className) {
    return myDelegate.getClassLocation(className);
  }

  @Override
  public String toString() {
    return myDelegate.toString();
  }

  @NotNull
  @Override
  public Collection<String> getAllClasses() {
    return myDelegate.getAllClasses();
  }

  @Override
  public boolean isEmpty() {
    return myDelegate.isEmpty();
  }
}
