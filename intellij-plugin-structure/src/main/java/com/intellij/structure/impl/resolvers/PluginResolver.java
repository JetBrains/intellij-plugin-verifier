package com.intellij.structure.impl.resolvers;

import com.google.common.base.Predicates;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.impl.domain.PluginManagerImpl;
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
import java.util.regex.Pattern;

/**
 * @author Sergey Patrikeev
 */
public class PluginResolver extends Resolver {

  private static final Logger LOG = LoggerFactory.getLogger(PluginResolver.class);
  private static final Pattern LIB_JAR_REGEX = Pattern.compile("([^/]+/)?lib/([^/]+\\.(jar|zip))");
  private static final Pattern CLASSES_DIR_REGEX = Pattern.compile("([^/]+/)?classes/");
  @NotNull private final File myPluginFile;
  @NotNull private final Plugin myPlugin;
  private final boolean myDeleteOnClose;
  private final Resolver myResolver;
  private volatile boolean isClosed;

  private PluginResolver(@NotNull Plugin plugin, @NotNull File extracted, boolean deleteOnClose) throws IncorrectPluginException {
    myPluginFile = extracted;
    myDeleteOnClose = deleteOnClose;
    myPlugin = plugin;
    try {
      myResolver = loadClasses(myPluginFile);
    } catch (Exception e) {
      if (myDeleteOnClose) {
        FileUtils.deleteQuietly(myPluginFile);
      }
      throw (e instanceof IncorrectPluginException ? ((IncorrectPluginException) e) : new RuntimeException(e));
    }
  }

  @NotNull
  public static PluginResolver createPluginResolver(@NotNull Plugin plugin) throws IncorrectPluginException, IOException {
    File file = plugin.getPluginFile();
    if (!file.exists()) {
      throw new IllegalArgumentException("Plugin file doesn't exist " + file);
    }
    if (file.isDirectory() || PluginManagerImpl.isJarOrZip(file)) {
      if (StringUtil.endsWithIgnoreCase(file.getName(), ".zip")) {
        File extracted = PluginExtractor.extractPlugin(plugin, file);
        try {
          return new PluginResolver(plugin, extracted, true);
        } catch (RuntimeException e) {
          FileUtils.deleteQuietly(extracted);
          throw e;
        }
      }
      return new PluginResolver(plugin, file, false);
    }
    throw new IllegalArgumentException("Incorrect plugin file type " + file.getName() + ": expected a directory, a .zip or a .jar archive");
  }

  @NotNull
  private static String getPluginDescriptor(Plugin plugin) {
    return plugin.getPluginId() + ":" + plugin.getPluginVersion();
  }

  @Override
  public void close() throws IOException {
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
  private Resolver loadClasses(@NotNull File file) throws IncorrectPluginException {
    if (file.isDirectory()) {
      return loadClassesFromDir(file);
    } else if (file.exists() && StringUtil.endsWithIgnoreCase(file.getName(), ".jar")) {
      return loadClassesFromJar(file);
    }
    throw new IncorrectPluginException("Invalid plugin file type: it must be a directory or a jar file");
  }

  @NotNull
  private Resolver loadClassesFromJar(@NotNull File file) {
    try {
      return Resolver.createJarResolver(file);
    } catch (IOException e) {
      throw new IncorrectPluginException("Unable to read the plugin " + myPlugin + " class files", e);
    }
  }

  private Resolver loadClassesFromDir(@NotNull File dir) throws IncorrectPluginException {
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

    Resolver rootResolver;
    try {
      String presentableName = "Plugin " + (classesDirExists ? "`classes`" : "root") + " directory of " + getPluginDescriptor(myPlugin);
      rootResolver = new FilesResolver(presentableName, root);
    } catch (IOException e) {
      throw new IncorrectPluginException("Unable to read " + (classesDirExists ? "`classes`" : "root") + " plugin classes", e);
    }
    List<Resolver> resolvers = new ArrayList<Resolver>();
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
    } catch (Exception e) {
      closeResolvers(resolvers);
      throw new IncorrectPluginException("Unable to read `lib` directory " + lib + " of the plugin " + myPlugin, e);
    }

    return Resolver.createUnionResolver("Plugin resolver " + myPlugin.getPluginId(), resolvers);
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
