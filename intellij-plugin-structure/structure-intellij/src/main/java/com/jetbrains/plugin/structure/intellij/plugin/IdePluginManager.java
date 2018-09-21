package com.jetbrains.plugin.structure.intellij.plugin;

import com.jetbrains.plugin.structure.base.plugin.*;
import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound;
import com.jetbrains.plugin.structure.base.problems.UnableToExtractZip;
import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor;
import com.jetbrains.plugin.structure.base.problems.UnexpectedDescriptorElements;
import com.jetbrains.plugin.structure.base.utils.FileUtilKt;
import com.jetbrains.plugin.structure.intellij.extractor.ExtractedPlugin;
import com.jetbrains.plugin.structure.intellij.extractor.ExtractorResult;
import com.jetbrains.plugin.structure.intellij.extractor.PluginExtractor;
import com.jetbrains.plugin.structure.intellij.problems.IncorrectIntellijFile;
import com.jetbrains.plugin.structure.intellij.problems.MultiplePluginDescriptorsInLibDirectory;
import com.jetbrains.plugin.structure.intellij.problems.PluginLibDirectoryIsEmpty;
import com.jetbrains.plugin.structure.intellij.problems.UnableToReadJarFile;
import com.jetbrains.plugin.structure.intellij.utils.JDOMUtil;
import com.jetbrains.plugin.structure.intellij.utils.StringUtil;
import com.jetbrains.plugin.structure.intellij.utils.URLUtil;
import com.jetbrains.plugin.structure.intellij.utils.xincludes.DefaultXIncludePathResolver;
import com.jetbrains.plugin.structure.intellij.utils.xincludes.XIncludePathResolver;
import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.jdom2.input.JDOMParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.jetbrains.plugin.structure.intellij.utils.StringUtil.toSystemIndependentName;

public final class IdePluginManager implements PluginManager<IdePlugin> {

  private static final Logger LOG = LoggerFactory.getLogger(IdePluginManager.class);

  public static final String PLUGIN_XML = "plugin.xml";
  public static final String META_INF = "META-INF";

  @NotNull
  private final XIncludePathResolver myPathResolver;

  @NotNull
  private final File myExtractDirectory;

  private IdePluginManager(@NotNull XIncludePathResolver pathResolver, @NotNull File extractDirectory) {
    myPathResolver = pathResolver;
    myExtractDirectory = extractDirectory;
  }

  @NotNull
  public static IdePluginManager createManager() {
    return createManager(new DefaultXIncludePathResolver());
  }

  @NotNull
  public static IdePluginManager createManager(@NotNull File extractDirectory) {
    return createManager(new DefaultXIncludePathResolver(), extractDirectory);
  }

  @NotNull
  public static IdePluginManager createManager(@NotNull XIncludePathResolver pathResolver) {
    return createManager(pathResolver, Settings.EXTRACT_DIRECTORY.getAsFile());
  }

  @NotNull
  public static IdePluginManager createManager(@NotNull XIncludePathResolver pathResolver, @NotNull File extractDirectory) {
    return new IdePluginManager(pathResolver, extractDirectory);
  }

  private PluginCreator loadPluginInfoFromJarFile(@NotNull File jarFile,
                                                  @NotNull final String descriptorPath,
                                                  @NotNull final XIncludePathResolver pathResolver,
                                                  boolean validateDescriptor) {
    ZipFile zipFile;
    try {
      zipFile = new ZipFile(jarFile);
    } catch (Exception e) {
      LOG.info("Unable to read jar file " + jarFile, e);
      return new PluginCreator(descriptorPath, new UnableToReadJarFile(), jarFile);
    }

    try {
      ZipEntry entry = getEntry(zipFile, descriptorPath);
      if (entry != null) {
        try (InputStream documentStream = zipFile.getInputStream(entry)) {
          Document document = JDOMUtil.loadDocument(documentStream);
          List<PluginIcon> icons = getIconsFromJarFile(zipFile);
          URL documentUrl = URLUtil.getJarEntryURL(jarFile, entry.getName());
          return new PluginCreator(descriptorPath, validateDescriptor, document, documentUrl, pathResolver, jarFile, icons);
        } catch (Exception e) {
          LOG.info("Unable to read file " + descriptorPath);
          return new PluginCreator(descriptorPath, new UnableToReadDescriptor(descriptorPath), jarFile);
        }
      } else {
        return new PluginCreator(descriptorPath, new PluginDescriptorIsNotFound(descriptorPath), jarFile);
      }
    } finally {
      try {
        zipFile.close();
      } catch (IOException e) {
        LOG.error("Unable to close jar file " + jarFile, e);
      }
    }
  }

  @NotNull
  private List<PluginIcon> getIconsFromJarFile(ZipFile jarFile) throws IOException {
    List<PluginIcon> icons = new ArrayList<>();
    for (IconTheme theme : IconTheme.values()) {
      String zipEntryName = getIconFileName(theme);
      ZipEntry entry = getEntry(jarFile, zipEntryName);
      if (entry == null) {
        continue;
      }
      byte[] iconContent = new byte[(int) entry.getSize()];
      IOUtils.readFully(jarFile.getInputStream(entry), iconContent);
      icons.add(new PluginIcon(theme, iconContent, zipEntryName));
    }
    return icons;
  }

  @Nullable
  private ZipEntry getEntry(@NotNull ZipFile zipFile, @NotNull String descriptorPath) {
    String zipEntryName = getZipEntryName(descriptorPath);
    ZipEntry entry = zipFile.getEntry(zipEntryName);
    return entry != null ? entry : zipFile.getEntry(zipEntryName.replace(File.separator, "/"));
  }

  @NotNull
  private String getZipEntryName(@NotNull String descriptorPath) {
    if (descriptorPath.startsWith("../")) {
      return StringUtil.trimStart(descriptorPath, "../");
    }
    return META_INF + File.separator + descriptorPath;
  }

  private PluginCreator loadPluginInfoFromDir(@NotNull File pluginDirectory, @NotNull String filePath, boolean validateDescriptor) {
    File descriptorFile = new File(pluginDirectory, META_INF + File.separator + filePath);
    if (!descriptorFile.exists()) {
      return loadPluginInfoFromLibDirectory(pluginDirectory, filePath, validateDescriptor);
    }

    try {
      URL documentUrl = URLUtil.fileToUrl(descriptorFile);
      Document document = JDOMUtil.loadDocument(documentUrl);
      List<PluginIcon> icons = loadIconsFromDir(new File(pluginDirectory, META_INF));
      return new PluginCreator(filePath, validateDescriptor, document, documentUrl, myPathResolver, pluginDirectory, icons);
    } catch (JDOMParseException e) {
      int lineNumber = e.getLineNumber();
      String message = lineNumber != -1 ? "unexpected element on line " + lineNumber : "unexpected elements";
      return new PluginCreator(filePath, new UnexpectedDescriptorElements(message, filePath), pluginDirectory);
    } catch (Exception e) {
      LOG.info("Unable to read plugin descriptor " + filePath + " of plugin " + descriptorFile, e);
      return new PluginCreator(filePath, new UnableToReadDescriptor(filePath), pluginDirectory);
    }
  }

  private List<PluginIcon> loadIconsFromDir(File metaInfDir) throws IOException {
    List<PluginIcon> icons = new ArrayList<>();
    for (IconTheme theme : IconTheme.values()) {
      File iconFile = new File(metaInfDir, getIconFileName(theme));
      if (!iconFile.exists()) {
        continue;
      }
      byte[] iconContent = new byte[(int) iconFile.length()];
      IOUtils.readFully(new FileInputStream(iconFile), iconContent);
      icons.add(new PluginIcon(theme, iconContent, iconFile.getName()));
    }
    return icons;
  }

  private PluginCreator loadPluginInfoFromLibDirectory(@NotNull final File root, @NotNull String descriptorPath, boolean validateDescriptor) {
    File libDir = new File(root, "lib");
    if (!libDir.isDirectory()) {
      return new PluginCreator(descriptorPath, new PluginDescriptorIsNotFound(descriptorPath), root);
    }

    final File[] files = libDir.listFiles();
    if (files == null || files.length == 0) {
      return new PluginCreator(descriptorPath, new PluginLibDirectoryIsEmpty(), root);
    }
    sortFilesWithRespectToRootDirectoryName(root, files);

    XIncludePathResolver pathResolver = new PluginXmlXIncludePathResolver(Arrays.asList(files));

    PluginCreator okOrPartiallyBrokenResult = null;

    for (final File file : files) {
      PluginCreator innerCreator;
      if (FileUtilKt.isJar(file) || FileUtilKt.isZip(file)) {
        innerCreator = loadPluginInfoFromJarFile(file, descriptorPath, pathResolver, validateDescriptor);
      } else if (file.isDirectory()) {
        innerCreator = loadPluginInfoFromDir(file, descriptorPath, validateDescriptor);
      } else {
        continue;
      }

      if (innerCreator.isSuccess() || innerCreator.hasOnlyInvalidDescriptorErrors()) {
        if (okOrPartiallyBrokenResult == null) {
          okOrPartiallyBrokenResult = innerCreator;
        } else {
          return getMultipleDescriptorsResult(root, descriptorPath, okOrPartiallyBrokenResult.getActualFile().getName(), innerCreator.getActualFile().getName());
        }
      }
    }
    if (okOrPartiallyBrokenResult != null) {
      return okOrPartiallyBrokenResult;
    }
    return new PluginCreator(descriptorPath, new PluginDescriptorIsNotFound(descriptorPath), root);
  }

  private PluginCreator getMultipleDescriptorsResult(File root, String descriptorPath, String firstDescriptor, String secondDescriptor) {
    if (firstDescriptor.compareTo(secondDescriptor) > 0) {
      String temp = firstDescriptor;
      firstDescriptor = secondDescriptor;
      secondDescriptor = temp;
    }
    return new PluginCreator(descriptorPath, new MultiplePluginDescriptorsInLibDirectory(firstDescriptor, secondDescriptor), root);
  }

  private void sortFilesWithRespectToRootDirectoryName(@NotNull final File root, File[] files) {
    //move plugin-jar to the beginning: Sample.jar goes first (if Sample is a plugin name)
    final String rootDirectoryName = root.getName();
    Arrays.sort(files, (o1, o2) -> {
      if (o2.getName().startsWith(rootDirectoryName)) return Integer.MAX_VALUE;
      if (o1.getName().startsWith(rootDirectoryName)) return -Integer.MAX_VALUE;
      if (o2.getName().startsWith("resources")) return -Integer.MAX_VALUE;
      if (o1.getName().startsWith("resources")) return Integer.MAX_VALUE;
      return 0;
    });
  }

  @NotNull
  private PluginCreator loadPluginInfoFromJarOrDirectory(@NotNull File jarOrDirectory,
                                                         @NotNull String descriptorPath,
                                                         boolean validateDescriptor) {
    descriptorPath = toSystemIndependentName(descriptorPath);
    PluginCreator pluginCreator;
    if (jarOrDirectory.isDirectory()) {
      pluginCreator = loadPluginInfoFromDir(jarOrDirectory, descriptorPath, validateDescriptor);
    } else if (FileUtilKt.isJar(jarOrDirectory)) {
      pluginCreator = loadPluginInfoFromJarFile(jarOrDirectory, descriptorPath, myPathResolver, validateDescriptor);
    } else {
      return new PluginCreator(descriptorPath, new IncorrectIntellijFile(jarOrDirectory.getName()), jarOrDirectory);
    }
    return resolveOptionalDependencies(jarOrDirectory, pluginCreator);
  }

  private PluginCreator resolveOptionalDependencies(@NotNull File jarOrDirectory,
                                                    @NotNull PluginCreator pluginCreator) {
    if (pluginCreator.isSuccess()) {
      Map<PluginDependency, String> optionalConfigurationFiles = pluginCreator.getOptionalDependenciesConfigurationFiles();
      return resolveOptionalDependencies(jarOrDirectory, optionalConfigurationFiles, pluginCreator);
    } else {
      return pluginCreator;
    }
  }

  private PluginCreator resolveOptionalDependencies(File jarOrDirectory,
                                                    Map<PluginDependency, String> optionalConfigurationFiles,
                                                    PluginCreator pluginCreator) {
    for (Map.Entry<PluginDependency, String> entry : optionalConfigurationFiles.entrySet()) {
      PluginDependency pluginDependency = entry.getKey();
      String configurationFile = entry.getValue();
      PluginCreator optionalCreator = resolveOptionalConfigurationFile(jarOrDirectory, configurationFile);
      pluginCreator.addOptionalDescriptor(pluginDependency, configurationFile, optionalCreator);
    }
    return pluginCreator;
  }

  private PluginCreator resolveOptionalConfigurationFile(@NotNull File jarOrDirectory, @NotNull String configurationFile) {
    if (configurationFile.startsWith("/" + META_INF + "/")) {
      configurationFile = StringUtil.trimStart(configurationFile, "/" + META_INF + "/");
    }
    return loadPluginInfoFromJarOrDirectory(jarOrDirectory, configurationFile, false);
  }

  @NotNull
  private PluginCreator extractZipAndCreatePlugin(@NotNull File zipPlugin,
                                                  boolean validateDescriptor) {
    ExtractorResult extractorResult;
    try {
      extractorResult = PluginExtractor.INSTANCE.extractPlugin(zipPlugin, myExtractDirectory);
    } catch (Exception e) {
      LOG.info("Unable to extract plugin zip " + zipPlugin, e);
      return new PluginCreator(PLUGIN_XML, new UnableToExtractZip(), zipPlugin);
    }
    if (extractorResult instanceof ExtractorResult.Success) {
      try (ExtractedPlugin extractedPlugin = ((ExtractorResult.Success) extractorResult).getExtractedPlugin()) {
        return loadPluginInfoFromJarOrDirectory(extractedPlugin.getPluginFile(), PLUGIN_XML, validateDescriptor);
      }
    } else {
      return new PluginCreator(PLUGIN_XML, ((ExtractorResult.Fail) extractorResult).getPluginProblem(), zipPlugin);
    }
  }

  @NotNull
  @Override
  public PluginCreationResult<IdePlugin> createPlugin(@NotNull File pluginFile) {
    return createPlugin(pluginFile, true);
  }

  @NotNull
  public PluginCreationResult<IdePlugin> createPlugin(@NotNull File pluginFile, boolean validateDescriptor) {
    return createPlugin(pluginFile, validateDescriptor, PLUGIN_XML);
  }

  @NotNull
  public PluginCreationResult<IdePlugin> createPlugin(@NotNull File pluginFile,
                                                      boolean validateDescriptor,
                                                      @NotNull String descriptorPath) {
    PluginCreator pluginCreator = getPluginCreatorWithResult(pluginFile, validateDescriptor, descriptorPath);
    pluginCreator.setOriginalFileAndExtractDir(pluginFile, myExtractDirectory);
    return pluginCreator.getPluginCreationResult();
  }

  @NotNull
  private PluginCreator getPluginCreatorWithResult(@NotNull File pluginFile,
                                                   boolean validateDescriptor,
                                                   @NotNull String descriptorPath) {
    if (!pluginFile.exists()) {
      throw new IllegalArgumentException("Plugin file " + pluginFile + " does not exist");
    }
    PluginCreator pluginCreator;
    if (FileUtilKt.isZip(pluginFile)) {
      pluginCreator = extractZipAndCreatePlugin(pluginFile, validateDescriptor);
    } else {
      pluginCreator = loadPluginInfoFromJarOrDirectory(pluginFile, descriptorPath, validateDescriptor);
    }
    return pluginCreator;
  }

  @NotNull
  private String getIconFileName(@NotNull IconTheme iconTheme) {
    return "pluginIcon" + iconTheme.getSuffix() + ".svg";
  }

}
