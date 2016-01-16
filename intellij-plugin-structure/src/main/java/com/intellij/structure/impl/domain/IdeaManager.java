package com.intellij.structure.impl.domain;

import com.google.common.base.Predicate;
import com.google.common.io.Files;
import com.intellij.structure.domain.Ide;
import com.intellij.structure.domain.IdeManager;
import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.impl.resolvers.CompileOutputResolver;
import com.intellij.structure.impl.utils.JarsUtils;
import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Sergey Patrikeev
 */
public class IdeaManager extends IdeManager {

  private static final IdeaManager INSTANCE = new IdeaManager();

  private static final Pattern BUILD_NUMBER_PATTERN = Pattern.compile("([^\\.]+\\.\\d+)\\.\\d+");

  private IdeaManager() {
  }

  public static IdeaManager getInstance() {
    return INSTANCE;
  }

  @NotNull
  private static IdeVersion readBuildNumber(@NotNull File versionFile) throws IOException {
    String buildNumberString = Files.toString(versionFile, Charset.defaultCharset()).trim();
    Matcher matcher = BUILD_NUMBER_PATTERN.matcher(buildNumberString);
    if (matcher.matches()) {
      return IdeVersion.createIdeVersion(matcher.group(1));
    }
    return IdeVersion.getDefaultVersion();
  }

  @NotNull
  private static Resolver getIdeaClassPoolFromLibraries(File ideaDir) throws IOException {
    final File lib = new File(ideaDir, "lib");
    final List<JarFile> jars = JarsUtils.getJars(lib, new Predicate<File>() {
      @Override
      public boolean apply(File file) {
        return !file.getName().endsWith("javac2.jar");
      }
    });

    return JarsUtils.makeClassPool(ideaDir.getPath(), jars);
  }

  @NotNull
  private static Resolver getIdeaClassPoolFromSources(File ideaDir) throws IOException {
    List<Resolver> pools = new ArrayList<Resolver>();

    pools.add(getIdeaClassPoolFromLibraries(ideaDir));

    if (new File(ideaDir, "community/.idea").isDirectory()) {
      pools.add(new CompileOutputResolver(new File(ideaDir, "out/classes/production")));
      pools.add(getIdeaClassPoolFromLibraries(new File(ideaDir, "community")));
    } else {
      pools.add(new CompileOutputResolver(new File(ideaDir, "out/production")));
    }

    return Resolver.getUnion(ideaDir.getPath(), pools);
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


  @NotNull
  @Override
  public Ide createIde(@NotNull File ideaDir) throws IOException, IncorrectPluginException {
    Resolver resolver;
    IdeVersion version = IdeVersion.getDefaultVersion();
    List<Plugin> bundled = new ArrayList<Plugin>();

    if (isSourceDir(ideaDir)) {
      resolver = getIdeaClassPoolFromSources(ideaDir);
      File versionFile = new File(ideaDir, "build.txt");
      if (!versionFile.exists()) {
        versionFile = new File(ideaDir, "community/build.txt");
      }
      if (versionFile.exists()) {
        version = readBuildNumber(versionFile);
      }
    } else {
      resolver = getIdeaClassPoolFromLibraries(ideaDir);
      bundled.addAll(getIdeaPlugins(ideaDir));
      version = readBuildNumber(new File(ideaDir, "build.txt"));
    }

    return new Idea(version, resolver, bundled);
  }
}
