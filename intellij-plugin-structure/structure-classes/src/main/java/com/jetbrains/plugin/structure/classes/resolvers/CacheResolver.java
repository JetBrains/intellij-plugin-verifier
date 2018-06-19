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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class CacheResolver extends Resolver {

  private static final int DEFAULT_CACHE_SIZE = 1024;

  private final Resolver myDelegate;

  private final LoadingCache<String, Optional<ClassNode>> myCache;

  public CacheResolver(@NotNull Resolver delegate) {
    this(delegate, DEFAULT_CACHE_SIZE);
  }

  public CacheResolver(@NotNull Resolver delegate, int cacheSize) {
    myDelegate = delegate;
    myCache = CacheBuilder.newBuilder()
        .maximumSize(cacheSize)
        .build(new CacheLoader<String, Optional<ClassNode>>() {
          @Override
          public Optional<ClassNode> load(String key) throws Exception {
            ClassNode classNode = myDelegate.findClass(key);
            if (classNode == null) {
              return Optional.empty();
            }
            return Optional.of(classNode);
          }
        });
  }

  @Override
  @Nullable
  public ClassNode findClass(@NotNull String className) throws IOException {
    try {
      return myCache.get(className).orElse(null);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof IOException) {
        throw (IOException) cause;
      }
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
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

  @NotNull
  @Override
  public Set<String> getAllPackages() {
    return myDelegate.getAllPackages();
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
