package com.jetbrains.plugin.structure.classes.resolvers;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class CacheResolver extends Resolver {

  private final Resolver myDelegate;
  /**
   * An exception indicating that a class file is not found in the @myDelegate resolver.
   * It lacks the stack trace which gives us less overhead.
   * It is necessary because the guava caches require to throw an exception if a key isn't found.
   */
  private final static Exception CLASS_NOT_FOUND_IN_CACHE_EXCEPTION = new Exception("Not found", null, false, false) {
  };
  private final LoadingCache<String, ClassNode> myCache;

  public CacheResolver(@NotNull Resolver delegate) {
    this(delegate, 1000);
  }

  public CacheResolver(@NotNull Resolver delegate, int cacheSize) {
    myDelegate = delegate;
    myCache = CacheBuilder.newBuilder()
        .maximumSize(cacheSize)
        .build(new CacheLoader<String, ClassNode>() {
          @Override
          public ClassNode load(String key) throws Exception {
            ClassNode classNode = myDelegate.findClass(key);
            if (classNode == null) {
              throw CLASS_NOT_FOUND_IN_CACHE_EXCEPTION;
            }
            return classNode;
          }
        });
  }

  @Override
  @Nullable
  public ClassNode findClass(@NotNull String className) throws IOException {
    try {
      return myCache.get(className);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (CLASS_NOT_FOUND_IN_CACHE_EXCEPTION == cause) {
        return null;
      }
      throw new IOException(e);
    }
  }

  @Override
  public Resolver getClassLocation(@NotNull String className) {
    return myDelegate.getClassLocation(className);
  }

  @Override
  public String toString() {
    return "Caching resolver for " + myDelegate.toString();
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

  @Override
  public boolean containsClass(@NotNull String className) {
    return myDelegate.containsClass(className);
  }

  @NotNull
  @Override
  public List<File> getClassPath() {
    return myDelegate.getClassPath();
  }

  @NotNull
  @Override
  public List<Resolver> getFinalResolvers() {
    return myDelegate.getFinalResolvers();
  }

  @Override
  public void close() throws IOException {
    myDelegate.close();
  }

  @Override
  public boolean processAllClasses(@NotNull Function1<? super ClassNode, Boolean> processor) throws IOException {
    return myDelegate.processAllClasses(processor);
  }
}
