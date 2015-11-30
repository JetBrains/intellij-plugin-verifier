package com.intellij.structure.domain;

import com.google.common.base.Predicate;
import com.google.common.io.Files;
import com.intellij.structure.errors.BrokenPluginException;
import com.intellij.structure.pool.ClassPool;
import com.intellij.structure.pool.CompileOutputPool;
import com.intellij.structure.pool.ContainerClassPool;
import com.intellij.structure.resolvers.CacheResolver;
import com.intellij.structure.resolvers.CombiningResolver;
import com.intellij.structure.resolvers.Resolver;
import com.intellij.structure.utils.Util;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Idea {
  private static final Pattern BUILD_NUMBER_PATTERN = Pattern.compile("([^\\.]+\\.\\d+)\\.\\d+");
  private final File myIdeaDir;
  private final JDK myJdk;
  private final ClassPool myClassPool;
  private final ClassPool myExternalClasspath;
  private final List<IdeaPlugin> myBundledPlugins;
  private final List<IdeaPlugin> myCustomPlugins = new ArrayList<IdeaPlugin>();
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
      myBundledPlugins = Collections.emptyList();
      File versionFile = new File(ideaDir, "build.txt");
      if (!versionFile.exists()) {
        versionFile = new File(ideaDir, "community/build.txt");
      }
      if (versionFile.exists()) {
        myVersion = readBuildNumber(versionFile);
      }
    } else {
      myClassPool = getIdeaClassPoolFromLibraries(ideaDir);
      myBundledPlugins = getIdeaPlugins();

      myVersion = readBuildNumber(new File(ideaDir, "build.txt"));
    }
  }

  private static String readBuildNumber(File versionFile) throws IOException {
    String buildNumberString = Files.toString(versionFile, Charset.defaultCharset()).trim();
    Matcher matcher = BUILD_NUMBER_PATTERN.matcher(buildNumberString);
    if (matcher.matches()) {
      return matcher.group(1);
    }
    return buildNumberString;
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

    if (new File(ideaDir, "community/.idea").isDirectory()) {
      pools.add(new CompileOutputPool(new File(ideaDir, "out/classes/production")));
      pools.add(getIdeaClassPoolFromLibraries(new File(ideaDir, "community")));
    } else {
      pools.add(new CompileOutputPool(new File(ideaDir, "out/production")));
    }

    return ContainerClassPool.getUnion(ideaDir.getPath(), pools);
  }

  private static boolean isSourceDir(File dir) {
    return new File(dir, "build").isDirectory()
        && new File(dir, "out").isDirectory()
        && new File(dir, ".git").isDirectory();
  }

  public String getVersion() {
    return myVersion;
  }

  public void setVersion(String myVersion) {
    this.myVersion = myVersion;
  }

  public void addCustomPlugin(IdeaPlugin plugin) {
    myCustomPlugins.add(plugin);
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
      } catch (BrokenPluginException e) {
        System.out.println("Failed to read plugin " + file + ": " + e.getMessage());
      }
    }

    return plugins;
  }

  public ClassPool getClassPool() {
    return myClassPool;
  }

  public String getMoniker() {
    return myIdeaDir.getPath();
  }

  public List<IdeaPlugin> getBundledPlugins() {
    return myBundledPlugins;
  }

  public IdeaPlugin getPlugin(String name) {
    for (IdeaPlugin plugin : myCustomPlugins) {
      if (plugin.getPluginId().equals(name)) {
        return plugin;
      }
    }
    for (IdeaPlugin plugin : myBundledPlugins) {
      if (plugin.getPluginId().equals(name))
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
      } else {
        resolverList = Arrays.asList(getClassPool(), myJdk.getResolver());
      }

      myResolver = new CacheResolver(CombiningResolver.union(resolverList));
    }

    return myResolver;
  }

  public IdeaPlugin getPluginByModule(String moduleId) {
    for (IdeaPlugin plugin : myCustomPlugins) {
      if (plugin.getDefinedModules().contains(moduleId)) {
        return plugin;
      }
    }
    for (IdeaPlugin plugin : myBundledPlugins) {
      if (plugin.getDefinedModules().contains(moduleId)) {
        return plugin;
      }
    }
    return null;
  }
}
