package com.intellij.structure.impl.resolvers;

import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.intellij.structure.impl.utils.JarsUtils;
import com.intellij.structure.impl.utils.PluginExtractor;
import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.resolvers.Resolver;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Sergey Patrikeev
 */
public class PluginResolver extends Resolver {

  private static final Logger LOG = LoggerFactory.getLogger(PluginResolver.class);
  @NotNull private final File myPluginFile;
  private final boolean myDeleteOnClose;
  private final Resolver myResolver;
  private boolean isClosed;

  public PluginResolver(@NotNull File extracted, boolean deleteOnClose) throws IOException {
    myPluginFile = extracted;
    myDeleteOnClose = deleteOnClose;
    try {
      myResolver = loadClasses(myPluginFile);
    } catch (Throwable e) {
      if (myDeleteOnClose) {
        FileUtils.deleteQuietly(myPluginFile);
      }
      throw Throwables.propagate(e);
    }
  }

  @NotNull
  public static Resolver createPluginResolver(@NotNull File pluginFile) throws IOException {
    if (!pluginFile.exists()) {
      throw new IllegalArgumentException("Plugin file doesn't exist " + pluginFile);
    }
    if (pluginFile.isDirectory() || JarsUtils.isJarOrZip(pluginFile)) {
      if (JarsUtils.isZip(pluginFile)) {
        File extracted = PluginExtractor.extractPlugin(pluginFile);
        try {
          return new PluginResolver(extracted, true);
        } catch (RuntimeException e) {
          FileUtils.deleteQuietly(extracted);
          throw e;
        }
      }
      return new PluginResolver(pluginFile, false);
    }
    throw new IllegalArgumentException("Incorrect plugin file type " + pluginFile + ": expected a directory, a .zip or a .jar archive");
  }

  @Override
  public synchronized void close() throws IOException {
    if (isClosed) {
      return;
    }
    isClosed = true;

    try {
      myResolver.close();
    } finally {
      if (myDeleteOnClose) {
        FileUtils.deleteQuietly(myPluginFile);
      }
    }
  }

  @Nullable
  @Override
  public ClassNode findClass(@NotNull String className) throws IOException {
    return myResolver.findClass(className);
  }

  @Nullable
  @Override
  public Resolver getClassLocation(@NotNull String className) {
    return myResolver.getClassLocation(className);
  }

  @NotNull
  @Override
  public Set<String> getAllClasses() {
    return myResolver.getAllClasses();
  }

  @Override
  public boolean isEmpty() {
    return myResolver.isEmpty();
  }

  @Override
  public boolean containsClass(@NotNull String className) {
    return myResolver.containsClass(className);
  }

  @NotNull
  @Override
  public List<File> getClassPath() {
    return myResolver.getClassPath();
  }

  @NotNull
  private Resolver loadClasses(@NotNull File file) throws IOException {
    if (file.isDirectory()) {
      return loadClassesFromDir(file);
    } else if (file.exists() && StringUtil.endsWithIgnoreCase(file.getName(), ".jar")) {
      return loadClassesFromJar(file);
    }
    throw new IllegalArgumentException("Invalid plugin file extension: " + file + ". It must be a directory or a jar file");
  }

  @NotNull
  private Resolver loadClassesFromJar(@NotNull File file) throws IOException {
    return Resolver.createJarResolver(file);
  }

  private Resolver loadClassesFromDir(@NotNull File dir) throws IOException {
    File classesDir = new File(dir, "classes");

    File root;
    boolean classesDirExists = classesDir.isDirectory();
    if (classesDirExists) {
      root = classesDir;
    } else {
      //it is possible that a plugin .zip-file is not actually a .zip archive, but a .jar archive (someone has renamed it)
      //so plugin classes will not be in the `classes` dir, but in the root dir itself
      root = dir;
    }

    List<Resolver> resolvers = new ArrayList<Resolver>();

    String presentableName = "Plugin " + (classesDirExists ? "`classes`" : "root") + " directory";
    Resolver rootResolver = new FilesResolver(presentableName, root);
    if (!rootResolver.isEmpty()) {
      resolvers.add(rootResolver);
    }


    File lib = new File(dir, "lib");
    try {
      if (lib.isDirectory()) {
        Collection<File> jars = JarsUtils.collectJars(lib, Predicates.<File>alwaysTrue(), false);
        Resolver libResolver = JarsUtils.makeResolver("Plugin `lib` jars: " + lib.getCanonicalPath(), jars);
        if (!libResolver.isEmpty()) {
          resolvers.add(libResolver);
        }
      }
    } catch (Throwable e) {
      closeResolvers(resolvers);
      Throwables.propagate(e);
    }

    return Resolver.createUnionResolver("Plugin resolver", resolvers);
  }

  private void closeResolvers(List<Resolver> resolvers) {
    for (Resolver resolver : resolvers) {
      try {
        resolver.close();
      } catch (Exception ce) {
        LOG.error("Unable to close resolver " + resolver, ce);
      }
    }
  }
}
