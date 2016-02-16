package com.intellij.structure.impl.domain;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.intellij.structure.impl.utils.JarsUtils;
import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

class Jdk implements com.intellij.structure.domain.Jdk {

  private static final Set<String> JDK_JAR_NAMES = ImmutableSet.of("rt.jar", "tools.jar", "classes.jar", "jsse.jar", "javaws.jar", "jce.jar");

  private final List<JarFile> myJars;
  private final Resolver myPool;

  Jdk(@NotNull File jdkDir) throws IOException {
    myJars = new ArrayList<JarFile>();

    collectJars(jdkDir);
    myPool = JarsUtils.makeClassPool(jdkDir.getPath(), myJars);
  }

  private void collectJars(@NotNull File dir) throws IOException {
    final List<JarFile> jars = JarsUtils.getJars(dir, new Predicate<File>() {
      @Override
      public boolean apply(File file) {
        return JDK_JAR_NAMES.contains(file.getName().toLowerCase());
      }
    });

    myJars.addAll(jars);

    final File[] files = dir.listFiles();
    if (files == null) {
      return;
    }

    for (File file : files) {
      if (file.isDirectory())
        collectJars(file);
    }
  }

  @Override
  @NotNull
  public Resolver getResolver() {
    return myPool;
  }

  @Override
  public String toString() {
    return myPool.toString();
  }
}
