package com.intellij.structure.domain;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.intellij.structure.pool.ClassPool;
import com.intellij.structure.resolvers.Resolver;
import com.intellij.structure.utils.Util;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

public class JDK {

  private static final Set<String> JDK_JAR_NAMES = ImmutableSet.of("rt.jar", "tools.jar", "classes.jar", "jsse.jar", "javaws.jar",
                                                                   "jce.jar");

  private final File myJdkDir;
  private final List<JarFile> myJars;
  private final ClassPool myPool;

  public JDK(final File jdkDir) throws IOException {
    myJdkDir = jdkDir;
    myJars = new ArrayList<JarFile>();

    collectJars(jdkDir);
    myPool = Util.makeClassPool(myJdkDir.getPath(), myJars);
  }

  private void collectJars(File dir) throws IOException {
    final List<JarFile> jars = Util.getJars(dir, new Predicate<File>() {
      @Override
      public boolean apply(File file) {
        return JDK_JAR_NAMES.contains(file.getName().toLowerCase());
      }
    });

    myJars.addAll(jars);

    final File[] files = dir.listFiles();
    if (files == null)
      return;

    for (File file : files) {
      if (file.isDirectory())
        collectJars(file);
    }
  }

  @NotNull
  public Resolver getResolver() {
    return myPool;
  }
}
