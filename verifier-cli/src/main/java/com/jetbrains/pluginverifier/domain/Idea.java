package com.jetbrains.pluginverifier.domain;

import com.google.common.base.Predicate;
import com.google.common.io.Files;
import com.jetbrains.pluginverifier.pool.ClassPool;
import com.jetbrains.pluginverifier.pool.CompileOutputPool;
import com.jetbrains.pluginverifier.pool.ContainerClassPool;
import com.jetbrains.pluginverifier.resolvers.CacheResolver;
import com.jetbrains.pluginverifier.resolvers.CombiningResolver;
import com.jetbrains.pluginverifier.resolvers.Resolver;
import com.jetbrains.pluginverifier.utils.Util;
import org.jdom.JDOMException;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;

public class Idea {
  private final File myIdeaDir;
  private final JDK myJdk;
  private final ClassPool myClassPool;
  private final ClassPool myExternalClasspath;
  private final List<IdeaPlugin> myPlugins;
  private Resolver myResolver;

  private String myVersion;

  public Idea(final File ideaDir, JDK jdk) throws IOException, JDOMException {
    this(ideaDir, jdk, null);
  }

  public Idea(final File ideaDir, JDK jdk, @Nullable ClassPool classpath) throws IOException, JDOMException {
    myIdeaDir = ideaDir;
    myJdk = jdk;
    myExternalClasspath = classpath;

    if (isSourceDir(ideaDir)) {
      myClassPool = getIdeaClassPoolFromSources(ideaDir);
      myPlugins = Collections.emptyList();
    }
    else {
      myClassPool = getIdeaClassPoolFromLibraries(ideaDir);
      myPlugins = getIdeaPlugins();

      myVersion = Files.toString(new File(ideaDir, "build.txt"), Charset.defaultCharset()).trim();
    }
  }

  public String getVersion() {
    return myVersion;
  }

  public void setVersion(String myVersion) {
    this.myVersion = myVersion;
  }

  private List<IdeaPlugin> getIdeaPlugins() throws JDOMException, IOException {
    final File pluginsDir = new File(myIdeaDir, "plugins");

    final File[] files = pluginsDir.listFiles();
    if (files == null)
      return Collections.emptyList();

    List<IdeaPlugin> plugins = new ArrayList<IdeaPlugin>();

    for (File file : files) {
      if (!file.isDirectory())
        continue;

      try {
        plugins.add(IdeaPlugin.createFromDirectory(file));
      }
      catch (BrokenPluginException e) {
        System.out.println("Failed to read plugin " + file + ": " + e.getMessage());
      }
    }

    return plugins;
  }

  private static ClassPool getIdeaClassPoolFromLibraries(File ideaDir) throws IOException {
    final File lib = new File(ideaDir, "lib");
    final List<JarFile> jars = Util.getJars(lib, new Predicate<File>() {
      @Override
      public boolean apply(File file) {
        return !file.getName().endsWith("javac2.jar");
      }
    });

    return Util.makeClassPool(ideaDir.getPath(), jars);
  }

  private static ClassPool getIdeaClassPoolFromSources(File ideaDir) throws IOException {
    List<ClassPool> pools = new ArrayList<ClassPool>();

    pools.add(getIdeaClassPoolFromLibraries(ideaDir));
    pools.add(new CompileOutputPool(new File(ideaDir, "out/classes/production")));

    File communityLib = new File(ideaDir, "community");
    if (communityLib.isDirectory()) {
      pools.add(getIdeaClassPoolFromLibraries(new File(ideaDir, "community")));
    }

    return ContainerClassPool.union(ideaDir.getPath(), pools);
  }

  private static boolean isSourceDir(File dir) {
    return new File(dir, "build").isDirectory()
           && new File(dir, "out").isDirectory()
           && new File(dir, ".git").isDirectory();
  }

  public ClassPool getClassPool()  {
    return myClassPool;
  }

  public String getMoniker() {
    return myIdeaDir.getPath();
  }

  public List<IdeaPlugin> getBundledPlugins() {
    return myPlugins;
  }

  public IdeaPlugin getBundledPlugin(String name)
  {
    for (IdeaPlugin plugin : myPlugins) {
      if (plugin.getId().equals(name))
        return plugin;
    }

    return null;
  }

  public File getIdeaDir() {
    return myIdeaDir;
  }

  public Resolver getResolver() {
    if (myResolver == null) {

      List<Resolver> resolverList;
      if (myExternalClasspath != null) {
        resolverList = Arrays.asList(getClassPool(),
                                     myJdk.getResolver(),
                                     myExternalClasspath);
      }
      else {
        resolverList = Arrays.asList(getClassPool(), myJdk.getResolver());
      }

      myResolver = new CacheResolver(CombiningResolver.union(resolverList));
    }

    return myResolver;
  }
}
