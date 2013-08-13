package com.jetbrains.pluginverifier.domain;

import com.jetbrains.pluginverifier.pool.ClassPool;
import com.jetbrains.pluginverifier.resolvers.ClassPoolResolver;
import com.jetbrains.pluginverifier.resolvers.CombiningResolver;
import com.jetbrains.pluginverifier.resolvers.Resolver;
import com.jetbrains.pluginverifier.util.Util;
import org.jdom.JDOMException;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;

public class Idea {
  private final File myIdeaDir;
  private final JDK myJdk;
  private final ClassPool myClassPool;
  private final ClassPool myExternalClasspath;
  private final List<IdeaPlugin> myPlugins;
  private Resolver myResolver;

  public Idea(final File ideaDir, JDK jdk) throws IOException, JDOMException {
    this(ideaDir, jdk, null);
  }

  public Idea(final File ideaDir, JDK jdk, @Nullable ClassPool classpath) throws IOException, JDOMException {
    myIdeaDir = ideaDir;
    myJdk = jdk;
    myExternalClasspath = classpath;
    myClassPool = getIdeaClassPool(ideaDir);
    myPlugins = getIdeaPlugins();
  }

  private List<IdeaPlugin> getIdeaPlugins() throws JDOMException, IOException {
    final File pluginsDir = new File(myIdeaDir, "plugins");

    List<IdeaPlugin> plugins = new ArrayList<IdeaPlugin>();

    final File[] files = pluginsDir.listFiles();
    if (files == null)
      return plugins;

    for (File file : files) {
      if (!file.isDirectory())
        continue;

      try {
        plugins.add(IdeaPlugin.createFromDirectory(this, file));
      }
      catch (BrokenPluginException ignored) {

      }
    }

    return plugins;
  }

  private static ClassPool getIdeaClassPool(File ideaDir) throws IOException {
    final File lib = new File(ideaDir, "lib");
    final List<JarFile> jars = Util.getJars(lib);

    for (JarFile jar : new ArrayList<JarFile>(jars)) {
      final String jarName = jar.getName().toLowerCase();

      if (jarName.endsWith("javac2.jar"))
        jars.remove(jar);
    }

    return Util.makeClassPool(ideaDir.getPath(), jars);
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
      if (myExternalClasspath != null) {
        myResolver = new CombiningResolver(Arrays.asList(new ClassPoolResolver(getClassPool()),
                                                         myJdk.getResolver(),
                                                         new ClassPoolResolver(myExternalClasspath)));
      }
      else {
        myResolver = new CombiningResolver(Arrays.asList(new ClassPoolResolver(getClassPool()), myJdk.getResolver()));
      }
    }

    return myResolver;
  }
}
