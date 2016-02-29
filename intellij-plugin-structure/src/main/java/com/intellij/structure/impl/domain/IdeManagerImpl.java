package com.intellij.structure.impl.domain;

import com.google.common.base.Predicate;
import com.google.common.io.Files;
import com.intellij.structure.domain.*;
import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.impl.resolvers.CompileOutputResolver;
import com.intellij.structure.impl.utils.JarsUtils;
import com.intellij.structure.impl.utils.xml.JDOMUtil;
import com.intellij.structure.resolvers.Resolver;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
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
  private static Resolver getIdeaResolverFromLibraries(File ideaDir) throws IOException {
    final File lib = new File(ideaDir, "lib");
    if (!lib.isDirectory()) {
      throw new IOException("Directory \"lib\" is not found (should be found at " + lib + ")");
    }

    final List<JarFile> jars = JarsUtils.getJars(lib, new Predicate<File>() {
      @Override
      public boolean apply(File file) {
        return !file.getName().endsWith("javac2.jar");
      }
    });

    return JarsUtils.makeResolver(ideaDir.getPath(), jars);
  }

  @NotNull
  private static Resolver getIdeaResolverFromSources(File ideaDir) throws IOException {
    List<Resolver> pools = new ArrayList<Resolver>();

    pools.add(getIdeaResolverFromLibraries(ideaDir));

    if (isUltimate(ideaDir)) {
      pools.add(new CompileOutputResolver(getUltimateClassesRoot(ideaDir)));
      pools.add(getIdeaResolverFromLibraries(new File(ideaDir, "community")));
    } else {
      pools.add(new CompileOutputResolver(getCommunityClassesRoot(ideaDir)));
    }

    return Resolver.getUnion(ideaDir.getPath(), pools);
  }

  @NotNull
  private static File getCommunityClassesRoot(File ideaDir) {
    return new File(ideaDir, "out/production");
  }

  @NotNull
  private static File getUltimateClassesRoot(File ideaDir) {
    return new File(ideaDir, "out/classes/production");
  }

  private static boolean isUltimate(File ideaDir) {
    return new File(ideaDir, "community/.idea").isDirectory();
  }

  @NotNull
  private static List<Plugin> getDummyPluginsFromSources(@NotNull File ideaDir) throws IOException {
    if (isUltimate(ideaDir)) {
      return getDummyPlugins(getUltimateClassesRoot(ideaDir));
    } else {
      return getDummyPlugins(getCommunityClassesRoot(ideaDir));
    }
  }

  @NotNull
  private static List<Plugin> getDummyPlugins(@NotNull File root) {
    List<Plugin> result = new ArrayList<Plugin>();
    Collection<File> files = FileUtils.listFiles(root, new WildcardFileFilter("plugin.xml"), TrueFileFilter.TRUE);
    for (File file : files) {
      InputStream is;
      try {
        is = FileUtils.openInputStream(file);
      } catch (IOException e) {
        continue;
      }

      try {
        Document xml = JDOMUtil.loadDocument(is);

        Plugin plugin;
        try {
          plugin = new PluginImpl(file, file.toURI().toURL(), Resolver.getEmptyResolver(), Resolver.getEmptyResolver(), xml);
        } catch (IncorrectPluginException e) {
          continue;
        } catch (IllegalArgumentException e) {
          continue;
        }

        result.add(plugin);

      } catch (JDOMException ignored) {
      } catch (MalformedURLException ignored) {
      } catch (IOException ignored) {
      } finally {
        IOUtils.closeQuietly(is);
      }
    }
    return result;
  }

  private static boolean isSourceDir(File dir) {
    return new File(dir, "build").isDirectory()
        && new File(dir, "out").isDirectory()
        && new File(dir, ".git").isDirectory();
  }

  @NotNull
  private static List<Plugin> getIdeaPlugins(File ideaDir) throws IOException {
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
        plugins.add(PluginManager.getInstance().createPlugin(file));
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
    return createIde(ideDir, null);
  }

  @NotNull
  @Override
  public Ide createIde(@NotNull File idePath, @Nullable IdeVersion version) throws IOException {
    Resolver resolver;
    List<Plugin> bundled = new ArrayList<Plugin>();

    if (isSourceDir(idePath)) {
      resolver = getIdeaResolverFromSources(idePath);
      File versionFile = new File(idePath, "build.txt");
      if (!versionFile.exists()) {
        versionFile = new File(idePath, "community/build.txt");
      }
      if (versionFile.exists()) {
        version = readBuildNumber(versionFile);
      }
      if (version == null) {
        throw new IncorrectPluginException("Unable to find IDE version file (build.txt or community/build.txt)");
      }
      bundled.addAll(getDummyPluginsFromSources(idePath));
    } else {
      resolver = getIdeaResolverFromLibraries(idePath);
      bundled.addAll(getIdeaPlugins(idePath));
      version = readBuildNumber(new File(idePath, "build.txt"));
    }

    return new IdeImpl(version, resolver, bundled);
  }
}
