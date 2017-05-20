package com.intellij.structure.impl.resolvers;

import com.intellij.structure.impl.utils.cache.LRUCache;
import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.List;

/**
 * @author Sergey Patrikeev
 */
public class CacheResolver extends Resolver {

  private static final int CLASSES_CACHE_SIZE = 1000;
  private final Resolver myDelegate;

  private final LRUCache<String, SoftReference<ClassNode>> myCache = new LRUCache<String, SoftReference<ClassNode>>(CLASSES_CACHE_SIZE);

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
  public Iterator<String> getAllClasses() {
    return myDelegate.getAllClasses();
  }

  @Override
  public boolean isEmpty() {
    return myDelegate.isEmpty();
  }

  @Override
  public boolean containsClass(@NotNull String className) {
    return myDelegate.containsClass(className);
  }

  @NotNull
  @Override
  public List<File> getClassPath() {
    return myDelegate.getClassPath();
  }

  @Override
  public void close() throws IOException {
    myDelegate.close();
  }
}
