package com.jetbrains.plugin.structure.impl.resolvers;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.jetbrains.plugin.structure.impl.utils.JarsUtils;
import com.jetbrains.plugin.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/**
 * @author Sergey Patrikeev
 */
public class JdkResolverCreator {
  private static final Set<String> JDK_JAR_NAMES = ImmutableSet.of("rt.jar", "tools.jar", "classes.jar", "jsse.jar", "javaws.jar", "jce.jar", "jfxrt.jar", "plugin.jar");

  @NotNull
  public static Resolver createJdkResolver(@NotNull File jdkDir) throws IOException {
    Collection<File> jars = JarsUtils.collectJars(jdkDir, new Predicate<File>() {
      @Override
      public boolean apply(File file) {
        return JDK_JAR_NAMES.contains(file.getName().toLowerCase());
      }
    }, true);

    return JarsUtils.makeResolver("Jdk resolver " + jdkDir.getCanonicalPath(), jars);
  }

}
