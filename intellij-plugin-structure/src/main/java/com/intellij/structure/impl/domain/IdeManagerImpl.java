package com.intellij.structure.impl.domain;

import com.google.common.io.Files;
import com.intellij.structure.domain.*;
import com.intellij.structure.impl.beans.PluginBean;
import com.intellij.structure.impl.beans.PluginBeanExtractor;
import com.intellij.structure.impl.beans.ReportingValidationEventHandler;
import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.impl.utils.validators.PluginXmlValidator;
import com.intellij.structure.impl.utils.validators.Validator;
import com.intellij.structure.impl.utils.xml.JDOMUtil;
import com.intellij.structure.impl.utils.xml.JDOMXIncluder;
import com.intellij.structure.impl.utils.xml.URLUtil;
import com.intellij.structure.impl.utils.xml.XIncludeException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.jdom2.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
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
    if (!versionFile.exists()) {
      throw new IllegalArgumentException(versionFile + " is not found");
    }
    String buildNumberString = Files.toString(versionFile, Charset.defaultCharset()).trim();
    return IdeVersion.createIdeVersion(buildNumberString);
  }

  public static boolean isUltimate(File ideaDir) {
    return new File(ideaDir, "community/.idea").isDirectory();
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
    } else {
      return getDummyPlugins(getCommunityClassesRoot(ideaDir));
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
    List<Plugin> result = new ArrayList<Plugin>();
    Collection<File> files = FileUtils.listFiles(root, new WildcardFileFilter("*.xml"), TrueFileFilter.TRUE);

    final Map<String, File> xmlDescriptors = new HashMap<String, File>();
    for (File file : files) {
      String path = file.getAbsolutePath();
      String[] parts = path.split("/");
      if (parts.length >= 2) {
        String key = "/" + parts[parts.length - 2] + "/" + parts[parts.length - 1];
        xmlDescriptors.put(key, file);
      }
    }

    JDOMXIncluder.PathResolver pathResolver = new PluginFromSourcePathResolver(xmlDescriptors);

    Validator dummyValidator = new PluginXmlValidator().ignoreMissingConfigElement().ignoreMissingFile();

    for (File file : files) {
      if (!file.getName().equals("plugin.xml")) {
        continue;
      }

      String relativePath = file.toURI().relativize(root.toURI()).getPath();

      File dummyRoot = file.getParentFile();
      if (dummyRoot != null) {
        dummyRoot = dummyRoot.getParentFile();
      }
      if (dummyRoot == null) {
        dummyRoot = file;
      }

      try {
        URL xmlUrl = file.toURI().toURL();
        Document documentByUrl = JDOMUtil.loadDocument(xmlUrl);

        Document document;
        try {
          document = PluginXmlExtractor.resolveXIncludes(documentByUrl, xmlUrl, pathResolver);
        } catch (Exception e) {
          LOG.warn("Unable to resolve XInclude elements", e);
          //let's try the document without the resolved xinclude elements
          document = documentByUrl;
        }

        PluginBean bean = PluginBeanExtractor.extractPluginBean(document, new ReportingValidationEventHandler(dummyValidator, relativePath));
        dummyValidator.validateBean(bean, relativePath);
        if (bean == null || dummyValidator.hasErrors()) {
          LOG.warn("Unable to load dummy plugin from " + relativePath);
          continue;
        }
        result.add(new PluginImpl(dummyRoot, document, bean));
      } catch (Exception e) {
        dummyValidator.onCheckedException("Unable to read XML document " + relativePath, e);
      }
    }
    return result;
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
      if (!file.isDirectory())
        continue;

      PluginCreationResult result = PluginManager.getInstance().createPlugin(file, false);
      if (result instanceof PluginCreationSuccess) {
        plugins.add(((PluginCreationSuccess) result).getPlugin());
      } else {
        for (PluginProblem problem : ((PluginCreationFail) result).getErrorsAndWarnings()) {
          LOG.warn("Failed to read plugin " + file + ". Problem: " + problem.getMessage());
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
        File versionFile = new File(idePath, "build.txt");
        if (!versionFile.exists()) {
          versionFile = new File(idePath, "community/build.txt");
        }
        if (versionFile.exists()) {
          version = readBuildNumber(versionFile);
        }
        if (version == null) {
          throw new IllegalArgumentException("Unable to find IDE version file (build.txt or community/build.txt)");
        }
      }
    } else {
      bundled.addAll(getIdeaPlugins(idePath));
      if (version == null) {
        version = readBuildNumber(new File(idePath, "build.txt"));
      }
    }

    return new IdeImpl(idePath, version, bundled);
  }

  private static class PluginFromSourcePathResolver extends JDOMXIncluder.DefaultPathResolver {
    private final Map<String, File> myDescriptors;

    PluginFromSourcePathResolver(Map<String, File> descriptors) {
      myDescriptors = descriptors;
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
        if (relativePath.startsWith("./")) {
          relativePath = "/META-INF/" + StringUtil.substringAfter(relativePath, "./");
        }

        File file = myDescriptors.get(relativePath);
        if (file != null) {
          try {
            return file.toURI().toURL();
          } catch (MalformedURLException exc) {
            throw new XIncludeException("File " + file + " has an invalid URL presentation ", exc);
          }
        }
      }
      throw new XIncludeException("Unable to resolve " + relativePath + (base != null ? " against " + base : ""));
    }
  }
}
