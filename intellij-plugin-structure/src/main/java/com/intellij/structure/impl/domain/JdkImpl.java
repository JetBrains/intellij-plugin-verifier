package com.intellij.structure.impl.domain;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.intellij.structure.domain.Jdk;
import com.intellij.structure.impl.utils.JarsUtils;
import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

public class JdkImpl extends Jdk {

  private static final Set<String> JDK_JAR_NAMES = ImmutableSet.of("rt.jar", "tools.jar", "classes.jar", "jsse.jar", "javaws.jar", "jce.jar", "jfxrt.jar", "plugin.jar");

  private final Resolver myPool;

  public JdkImpl(@NotNull File jdkDir) throws IOException {
    Collection<File> jars = JarsUtils.collectJarsRecursively(jdkDir, new Predicate<File>() {
      @Override
      public boolean apply(File file) {
        return JDK_JAR_NAMES.contains(file.getName().toLowerCase());
      }
    });

    myPool = JarsUtils.makeResolver("Jdk resolver " + jdkDir.getCanonicalPath(), jars);
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
