package com.intellij.structure.impl.domain;

import com.google.common.base.Predicate;
import com.google.common.io.Files;
import com.intellij.structure.domain.Ide;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.impl.pool.CompileOutputPool;
import com.intellij.structure.impl.pool.ContainerClassPool;
import com.intellij.structure.impl.utils.JarsUtils;
import com.intellij.structure.pool.ClassPool;
import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Idea implements Ide {
  private static final Pattern BUILD_NUMBER_PATTERN = Pattern.compile("([^\\.]+\\.\\d+)\\.\\d+");
  private final ClassPool myClassPool;
  private final List<Plugin> myBundledPlugins = new ArrayList<Plugin>();
  private final List<Plugin> myCustomPlugins = new ArrayList<Plugin>();
  private Resolver myResolver;

  private String myVersion;

  Idea(@NotNull File ideaDir) throws IOException, IncorrectPluginException {
    if (isSourceDir(ideaDir)) {
      myClassPool = getIdeaClassPoolFromSources(ideaDir);
      File versionFile = new File(ideaDir, "build.txt");
      if (!versionFile.exists()) {
        versionFile = new File(ideaDir, "community/build.txt");
      }
      if (versionFile.exists()) {
        myVersion = readBuildNumber(versionFile);
      }
    } else {
      myClassPool = getIdeaClassPoolFromLibraries(ideaDir);
      myBundledPlugins.addAll(getIdeaPlugins(ideaDir));
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
    final List<JarFile> jars = JarsUtils.getJars(lib, new Predicate<File>() {
      @Override
      public boolean apply(File file) {
        return !file.getName().endsWith("javac2.jar");
      }
    });

    return JarsUtils.makeClassPool(ideaDir.getPath(), jars);
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

  @NotNull
  private static List<Plugin> getIdeaPlugins(File ideaDir) throws IOException, IncorrectPluginException {
    final File pluginsDir = new File(ideaDir, "plugins");

    final File[] files = pluginsDir.listFiles();
    if (files == null) {
      return Collections.emptyList();
    }

    List<Plugin> plugins = new ArrayList<Plugin>();

    for (File file : files) {
      if (!file.isDirectory())
        continue;

      try {
        plugins.add(IdeaPluginManager.getInstance().createPlugin(file));
      } catch (IncorrectPluginException e) {
        System.out.println("Failed to read plugin " + file + ": " + e.getMessage());
      } catch (IOException e) {
        System.out.println("Failed to read plugin " + file + ": " + e.getMessage());
      }
    }

    return plugins;
  }

  @Override
  @NotNull
  public String getVersion() {
    return myVersion;
  }

  @Override
  public void updateVersion(@NotNull String newVersion) {
    myVersion = newVersion;
  }

  @Override
  public void addCustomPlugin(@NotNull Plugin plugin) {
    myCustomPlugins.add(plugin);
  }

  @Override
  @NotNull
  public List<Plugin> getCustomPlugins() {
    return myCustomPlugins;
  }

  @Override
  @NotNull
  public List<Plugin> getBundledPlugins() {
    return myBundledPlugins;
  }

  @Override
  @Nullable
  public Plugin getPluginById(@NotNull String pluginId) {
    for (Plugin plugin : myCustomPlugins) {
      if (plugin.getPluginId().equals(pluginId)) {
        return plugin;
      }
    }
    for (Plugin plugin : myBundledPlugins) {
      if (plugin.getPluginId().equals(pluginId))
        return plugin;
    }
    return null;
  }


  @NotNull
  @Override
  public ClassPool getClassPool() {
    return myClassPool;
  }

  @Override
  @Nullable
  public Plugin getPluginByModule(@NotNull String moduleId) {
    for (Plugin plugin : myCustomPlugins) {
      if (plugin.getDefinedModules().contains(moduleId)) {
        return plugin;
      }
    }
    for (Plugin plugin : myBundledPlugins) {
      if (plugin.getDefinedModules().contains(moduleId)) {
        return plugin;
      }
    }
    return null;
  }
}
