package com.intellij.structure.impl.resolvers;

import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Sergey Evdokimov
 */
public class CacheResolver extends Resolver {

  private final Resolver myDelegate;

  private final Map<String, SoftReference<ClassNode>> myCache = new HashMap<String, SoftReference<ClassNode>>();

  public CacheResolver(@NotNull Resolver delegate) {
    myDelegate = delegate;
  }

  @Override
  @Nullable
  public ClassNode findClass(@NotNull String className) throws IOException {
    SoftReference<ClassNode> reference = myCache.get(className);
    ClassNode node = reference == null ? null : reference.get();
    if (node == null) {
      node = myDelegate.findClass(className);
      myCache.put(className, new SoftReference<ClassNode>(node));
    }
    return node;
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
