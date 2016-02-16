package com.intellij.structure.impl.domain;

import com.google.common.base.Predicate;
import com.google.common.io.Files;
import com.intellij.structure.domain.*;
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
public class IdeManagerImpl extends IdeManager {

  private static final Pattern BUILD_NUMBER_PATTERN = Pattern.compile("([^\\.]+\\.\\d+)\\.\\d+");

  @NotNull
  private static IdeVersion readBuildNumber(@NotNull File versionFile) throws IOException {
    String buildNumberString = Files.toString(versionFile, Charset.defaultCharset()).trim();
    Matcher matcher = BUILD_NUMBER_PATTERN.matcher(buildNumberString);
    if (matcher.matches()) {
      //IU-144.1532.23 -->> IU-144.1532 (without build number)
      return IdeVersion.createIdeVersion(matcher.group(1));
    }
    return IdeVersion.createIdeVersion(buildNumberString);
  }

  @NotNull
  private static Resolver getIdeClassPoolFromLibraries(File ideDir) throws IOException {
    final File lib = new File(ideDir, "lib");
    if (!lib.isDirectory()) {
      throw new IOException("Directory \"lib\" is not found (should be found at " + lib + ")");
    }

    final List<JarFile> jars = JarsUtils.getJars(lib, new Predicate<File>() {
      @Override
      public boolean apply(File file) {
        return !file.getName().endsWith("javac2.jar");
      }
    });

    return JarsUtils.makeClassPool(ideDir.getPath(), jars);
  }

  @NotNull
  private static Resolver getIdeaClassPoolFromSources(File ideDir) throws IOException {
    List<Resolver> pools = new ArrayList<Resolver>();

    pools.add(getIdeClassPoolFromLibraries(ideDir));

    if (new File(ideDir, "community/.idea").isDirectory()) {
      pools.add(new CompileOutputResolver(new File(ideDir, "out/classes/production")));
      pools.add(getIdeClassPoolFromLibraries(new File(ideDir, "community")));
    } else {
      pools.add(new CompileOutputResolver(new File(ideDir, "out/production")));
    }

    return Resolver.getUnion(ideDir.getPath(), pools);
  }

  private static boolean isSourceDir(File dir) {
    return new File(dir, "build").isDirectory()
        && new File(dir, "out").isDirectory()
        && new File(dir, ".git").isDirectory();
  }

  @NotNull
  private static List<Plugin> getIdePlugins(File ideDir) throws IOException {
    final File pluginsDir = new File(ideDir, "plugins");

    final File[] files = pluginsDir.listFiles();
    if (files == null) {
      return Collections.emptyList();
    }

    List<Plugin> plugins = new ArrayList<Plugin>();

    for (File file : files) {
      if (!file.isDirectory())
        continue;

      try {
        plugins.add(PluginManager.getPluginManager().createPlugin(file));
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
  public Ide createIde(@NotNull File ideDir) throws IOException {
    Resolver resolver;
    IdeVersion version = null;
    List<Plugin> bundled = new ArrayList<Plugin>();

    if (isSourceDir(ideDir)) {
      resolver = getIdeaClassPoolFromSources(ideDir);
      File versionFile = new File(ideDir, "build.txt");
      if (!versionFile.exists()) {
        versionFile = new File(ideDir, "community/build.txt");
      }
      if (versionFile.exists()) {
        version = readIdeVersion(versionFile);
      }
      if (version == null) {
        throw new IllegalArgumentException("Unable to find IDE version file (build.txt or community/build.txt)");
      }
    } else {
      resolver = getIdeClassPoolFromLibraries(ideDir);
      bundled.addAll(getIdePlugins(ideDir));
      version = readIdeVersion(new File(ideDir, "build.txt"));
    }

    return new IdeImpl(version, resolver, bundled);
  }

  @NotNull
  private IdeVersion readIdeVersion(@NotNull File versionFile) {
    IdeVersion version;
    try {
      version = readBuildNumber(versionFile);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("IDE version is invalid", e);
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to read IDE version", e);
    }
    return version;
  }
}
