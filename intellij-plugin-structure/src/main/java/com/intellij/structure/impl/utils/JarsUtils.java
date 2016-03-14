package com.intellij.structure.impl.utils;

import com.google.common.base.Predicate;
import com.intellij.structure.resolvers.Resolver;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

public class JarsUtils {

  @NotNull
  public static List<ZipFile> collectJarsRecursively(@NotNull File directory, @NotNull final Predicate<File> filter) throws IOException {

    Collection<File> allJars = FileUtils.listFiles(directory, new AbstractFileFilter() {
      @Override
      public boolean accept(File file) {
        return StringUtil.endsWithIgnoreCase(file.getName(), ".jar") && filter.apply(file);
      }
    }, TrueFileFilter.INSTANCE);

    List<ZipFile> jarFiles = new ArrayList<ZipFile>();

    for (File jar : allJars) {
      try {
        jarFiles.add(new JarFile(jar, false));
      } catch (IOException e) {
        throw new IOException("Failed to open jar file " + jar, e);
      }
    }

    return jarFiles;
  }

  @NotNull
  public static Resolver makeResolver(@NotNull String presentableName, @NotNull List<ZipFile> jars) throws IOException {
    List<Resolver> pool = new ArrayList<Resolver>();

    for (ZipFile jar : jars) {
      try {
        pool.add(Resolver.createJarResolver(jar));
      } catch (IOException e) {
        throw new IOException("Unable to create resolver for " + jar.getName(), e);
      }
    }

    return Resolver.createUnionResolver(presentableName, pool);
  }

}
