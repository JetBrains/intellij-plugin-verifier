package com.jetbrains.plugin.structure.impl.utils;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.jetbrains.plugin.structure.resolvers.Resolver;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JarsUtils {

  private static final Logger LOG = LoggerFactory.getLogger(JarsUtils.class);

  @NotNull
  public static Collection<File> collectJars(@NotNull File directory, @NotNull final Predicate<File> filter, boolean recursively) {
    return FileUtils.listFiles(directory, new AbstractFileFilter() {
      @Override
      public boolean accept(File file) {
        return StringUtil.endsWithIgnoreCase(file.getName(), ".jar") && filter.apply(file);
      }
    }, recursively ? TrueFileFilter.INSTANCE : FalseFileFilter.FALSE);
  }

  @NotNull
  public static Resolver makeResolver(@NotNull String presentableName, @NotNull Collection<File> jars) throws IOException {
    List<Resolver> pool = new ArrayList<Resolver>();

    for (File jar : jars) {
      if (!jar.exists()) {
        closeResolvers(pool);
        throw new IllegalArgumentException("File " + jar + " doesn't exist");
      }
      try {
        pool.add(Resolver.createJarResolver(jar));
      } catch (Throwable e) {
        closeResolvers(pool);
        Throwables.propagate(e);
      }
    }

    return Resolver.createUnionResolver(presentableName, pool);
  }

  private static void closeResolvers(List<Resolver> pool) {
    for (Resolver opened : pool) {
      try {
        opened.close();
      } catch (Exception ce) {
        LOG.error("Unable to close resolver " + opened, ce);
      }
    }
  }

}
