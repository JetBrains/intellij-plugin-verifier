package com.intellij.structure.impl.domain;

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import com.intellij.structure.ide.Ide;
import com.intellij.structure.ide.IdeManager;
import com.intellij.structure.ide.IdeVersion;
import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.impl.utils.xml.JDOMXIncluder;
import com.intellij.structure.impl.utils.xml.URLUtil;
import com.intellij.structure.impl.utils.xml.XIncludeException;
import com.intellij.structure.plugin.Plugin;
import com.intellij.structure.plugin.PluginCreationFail;
import com.intellij.structure.plugin.PluginCreationResult;
import com.intellij.structure.plugin.PluginCreationSuccess;
import com.intellij.structure.problems.PluginProblem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

/**
 * @author Sergey Patrikeev
 */
public class IdeManagerImpl extends IdeManager {

  private static final Logger LOG = LoggerFactory.getLogger(IdeManagerImpl.class);

  @NotNull
  private static IdeVersion readBuildNumber(@NotNull File versionFile) throws IOException {
    String buildNumberString = Files.toString(versionFile, Charset.defaultCharset()).trim();
    return IdeVersion.createIdeVersion(buildNumberString);
  }

  public static boolean isUltimate(File ideaDir) {
    return new File(ideaDir, "community/.idea").isDirectory() && getUltimateClassesRoot(ideaDir).isDirectory();
  }

  public static boolean isCommunity(File ideaDir) {
    return new File(ideaDir, ".idea").isDirectory() && getCommunityClassesRoot(ideaDir).isDirectory();
  }

  @NotNull
  public static File getUltimateClassesRoot(File ideaDir) {
    return new File(ideaDir, "out/classes/production");
  }

  @NotNull
  public static File getCommunityClassesRoot(File ideaDir) {
    return new File(ideaDir, "out/production");
  }

  @NotNull
  private static List<Plugin> getDummyPluginsFromSources(@NotNull File ideaDir) throws IOException {
    if (isUltimate(ideaDir)) {
      return getDummyPlugins(getUltimateClassesRoot(ideaDir));
    } else if (isCommunity(ideaDir)) {
      return getDummyPlugins(getCommunityClassesRoot(ideaDir));
    } else {
      throw new IllegalArgumentException("Incorrect IDEA structure: " + ideaDir + ". It must be Community or Ultimate sources root with compiled class files.");
    }
  }

  /**
   * Creates plugin descriptors from the found plugin.xml files.
   * We don't know exactly which classes correspond to which descriptors
   * so these classes are all in one Resolver.
   * <p>See {@link com.intellij.structure.impl.resolvers.IdeResolverCreator#getIdeaResolverFromSources(File)}</p>
   *
   * @param root idea root directory
   * @return dummy (no classes) plugins
   */

  @NotNull
  private static List<Plugin> getDummyPlugins(@NotNull File root) {
    Collection<File> xmlFiles = FileUtils.listFiles(root, new WildcardFileFilter("*.xml"), TrueFileFilter.TRUE);

    JDOMXIncluder.PathResolver pathResolver = getFromSourcesPathResolver(xmlFiles);
    return getDummyPlugins(xmlFiles, pathResolver);
  }

  @NotNull
  private static List<Plugin> getDummyPlugins(@NotNull Collection<File> xmlFiles,
                                              @NotNull JDOMXIncluder.PathResolver pathResolver) {
    List<Plugin> result = new ArrayList<Plugin>();
    for (File xmlFile : xmlFiles) {
      if ("plugin.xml".equals(xmlFile.getName())) {
        File metaInf = xmlFile.getAbsoluteFile().getParentFile();
        if ("META-INF".equals(metaInf.getName()) && metaInf.isDirectory() && metaInf.getParentFile() != null) {
          File pluginDirectory = metaInf.getParentFile();
          if (pluginDirectory.isDirectory()) {
            try {
              PluginCreator pluginCreator = new PluginManagerImpl(pathResolver).getPluginCreatorWithResult(pluginDirectory, false);
              pluginCreator.setOriginalFile(pluginDirectory);
              PluginCreationResult creationResult = pluginCreator.getPluginCreationResult();
              if (creationResult instanceof PluginCreationSuccess) {
                result.add(((PluginCreationSuccess) creationResult).getPlugin());
              }
            } catch (Exception e) {
              LOG.debug("Unable to create plugin from sources: " + pluginDirectory, e);
            }
          }
        }
      }
    }
    return result;
  }

  @NotNull
  private static JDOMXIncluder.PathResolver getFromSourcesPathResolver(Collection<File> xmlFiles) {
    final Map<String, File> xmlDescriptors = new HashMap<String, File>();
    for (File file : xmlFiles) {
      String path = file.getAbsolutePath();
      String[] parts = path.split("/");
      if (parts.length >= 2) {
        String key = "/" + parts[parts.length - 2] + "/" + parts[parts.length - 1];
        xmlDescriptors.put(key, file);
      }
    }
    return new PluginFromSourcePathResolver(xmlDescriptors);
  }

  public static boolean isSourceDir(File dir) {
    return new File(dir, ".idea").isDirectory();
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
      if (file.isDirectory()) {
        PluginCreator pluginCreator = new PluginManagerImpl().getPluginCreatorWithResult(file, false);
        pluginCreator.setOriginalFile(file);
        PluginCreationResult result = pluginCreator.getPluginCreationResult();
        if (result instanceof PluginCreationSuccess) {
          plugins.add(((PluginCreationSuccess) result).getPlugin());
        } else {
          List<PluginProblem> problems = ((PluginCreationFail) result).getErrorsAndWarnings();
          LOG.warn("Failed to read plugin " + file + ". Problems: " + Joiner.on(", ").join(problems));
        }
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
    if (!idePath.exists()) {
      throw new IllegalArgumentException("IDE file " + idePath + " is not found");
    }
    List<Plugin> bundled = new ArrayList<Plugin>();

    if (isSourceDir(idePath)) {
      bundled.addAll(getDummyPluginsFromSources(idePath));
      if (version == null) {
        version = readVersionFromSourcesDir(idePath);
      }
    } else {
      bundled.addAll(getIdeaPlugins(idePath));
      if (version == null) {
        version = readVersionFromBinaries(idePath);
      }
    }

    return new IdeImpl(idePath, version, bundled);
  }

  @NotNull
  private IdeVersion readVersionFromBinaries(@NotNull File idePath) throws IOException {
    File versionFile = new File(idePath, "build.txt");
    if (!versionFile.exists()) {
      throw new IllegalArgumentException(versionFile + " is not found");
    }
    return readBuildNumber(versionFile);
  }

  @NotNull
  private IdeVersion readVersionFromSourcesDir(@NotNull File idePath) throws IOException {
    File versionFile = new File(idePath, "build.txt");
    if (!versionFile.exists()) {
      versionFile = new File(idePath, "community/build.txt");
      if (!versionFile.exists()) {
        throw new IllegalArgumentException("Unable to find IDE version file (build.txt or community/build.txt)");
      }
    }
    return readBuildNumber(versionFile);
  }

  private static class PluginFromSourcePathResolver extends JDOMXIncluder.DefaultPathResolver {
    private final Map<String, File> myDescriptors;

    PluginFromSourcePathResolver(Map<String, File> descriptors) {
      myDescriptors = descriptors;
    }

    @NotNull
    private URL resolveOutputDirectories(@NotNull String relativePath, @Nullable String base) {
      if (relativePath.startsWith("./")) {
        relativePath = "/META-INF/" + StringUtil.substringAfter(relativePath, "./");
      }

      File file = myDescriptors.get(relativePath);
      if (file != null) {
        try {
          return file.toURI().toURL();
        } catch (Exception exc) {
          throw new XIncludeException("File " + file + " has an invalid URL presentation ", exc);
        }
      }
      throw new XIncludeException("Unable to resolve " + relativePath + (base != null ? " against " + base : ""));
    }

    @NotNull
    @Override
    public URL resolvePath(@NotNull String relativePath, @Nullable String base) {
      try {
        URL res = super.resolvePath(relativePath, base);
        //try the parent resolver
        URLUtil.openStream(res);
        return res;
      } catch (IOException e) {
        return resolveOutputDirectories(relativePath, base);
      }
    }
  }
}
