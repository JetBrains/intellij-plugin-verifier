package com.intellij.structure.impl.domain;

import com.intellij.structure.impl.extractor.*;
import com.intellij.structure.impl.utils.JarsUtils;
import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.impl.utils.xml.JDOMUtil;
import com.intellij.structure.impl.utils.xml.JDOMXIncluder;
import com.intellij.structure.impl.utils.xml.URLUtil;
import com.intellij.structure.plugin.PluginCreationResult;
import com.intellij.structure.plugin.PluginDependency;
import com.intellij.structure.plugin.PluginManager;
import com.intellij.structure.problems.*;
import org.jdom2.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.intellij.structure.impl.utils.StringUtil.toSystemIndependentName;

/**
 * @author Sergey Patrikeev
 */
public class PluginManagerImpl extends PluginManager {

  private static final Logger LOG = LoggerFactory.getLogger(PluginManagerImpl.class);
  private static final String PLUGIN_XML = "plugin.xml";

  private final JDOMXIncluder.PathResolver myPathResolver;

  public PluginManagerImpl() {
    myPathResolver = PluginXmlExtractor.DEFAULT_PLUGIN_XML_PATH_RESOLVER;
  }

  public PluginManagerImpl(JDOMXIncluder.PathResolver pathResolver) {
    myPathResolver = pathResolver;
  }

  private PluginCreator loadDescriptorFromJarFile(@NotNull File jarFile,
                                                  @NotNull final String descriptorPath,
                                                  @NotNull final JDOMXIncluder.PathResolver pathResolver,
                                                  boolean validateDescriptor) {
    ZipFile zipFile;
    try {
      zipFile = new ZipFile(jarFile);
    } catch (Exception e) {
      LOG.debug("Unable to read jar file " + jarFile, e);
      return new PluginCreator(descriptorPath, new UnableToReadJarFile(jarFile), jarFile);
    }

    try {
      ZipEntry entry = getEntry(zipFile, descriptorPath);
      if (entry != null) {
        try {
          URL documentUrl = URLUtil.getJarEntryURL(jarFile, entry.getName());
          InputStream documentStream = zipFile.getInputStream(entry);
          Document document = JDOMUtil.loadDocument(documentStream);
          return new PluginCreator(descriptorPath, validateDescriptor, document, documentUrl, pathResolver, jarFile);
        } catch (Exception e) {
          LOG.debug("Unable to read file " + descriptorPath);
          return new PluginCreator(descriptorPath, new UnableToReadDescriptor(descriptorPath), jarFile);
        }
      } else {
        return new PluginCreator(descriptorPath, new PluginDescriptorIsNotFound(descriptorPath), jarFile);
      }
    } finally {
      try {
        zipFile.close();
      } catch (IOException e) {
        LOG.debug("Unable to close jar file " + jarFile, e);
      }
    }
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
    return "META-INF" + File.separator + descriptorPath;
  }

  private PluginCreator loadDescriptorFromDir(@NotNull File pluginDirectory, @NotNull String filePath, boolean validateDescriptor) {
    File descriptorFile = new File(pluginDirectory, "META-INF" + File.separator + filePath);
    if (descriptorFile.exists()) {
      return loadDescriptorFromDescriptorFile(filePath, pluginDirectory, descriptorFile, validateDescriptor);
    } else {
      return loadDescriptorFromLibDirectory(pluginDirectory, filePath, validateDescriptor);
    }
  }

  private PluginCreator loadDescriptorFromDescriptorFile(@NotNull String descriptorPath,
                                                         @NotNull File pluginDirectory,
                                                         @NotNull File descriptorFile,
                                                         boolean validateDescriptor) {
    try {
      URL documentUrl = URLUtil.fileToUrl(descriptorFile);
      Document document = JDOMUtil.loadDocument(documentUrl);
      return new PluginCreator(descriptorPath, validateDescriptor, document, documentUrl, myPathResolver, pluginDirectory);
    } catch (Exception e) {
      LOG.debug("Unable to read plugin descriptor " + descriptorFile, e);
      return new PluginCreator(descriptorPath, new UnableToReadDescriptor(descriptorFile.getPath()), pluginDirectory);
    }
  }

  private PluginCreator loadDescriptorFromLibDirectory(@NotNull final File root, @NotNull String descriptorPath, boolean validateDescriptor) {
    File libDir = new File(root, "lib");
    if (!libDir.isDirectory()) {
      return new PluginCreator(descriptorPath, new PluginDescriptorIsNotFound(descriptorPath), root);
    }

    final File[] files = libDir.listFiles();
    if (files == null || files.length == 0) {
      return new PluginCreator(descriptorPath, new PluginLibDirectoryIsEmpty(libDir), root);
    }
    sortFilesWithRespectToRootDirectoryName(root, files);

    List<URL> metaInfUrls = getInLibMetaInfUrls(files);
    JDOMXIncluder.PathResolver pathResolver = new PluginXmlExtractor.PluginXmlPathResolver(metaInfUrls);

    PluginCreator result = null;

    for (final File file : files) {
      PluginCreator innerCreator;
      if (JarsUtils.isJarOrZip(file)) {
        innerCreator = loadDescriptorFromJarFile(file, descriptorPath, pathResolver, validateDescriptor);
      } else if (file.isDirectory()) {
        innerCreator = loadDescriptorFromDir(file, descriptorPath, validateDescriptor);
      } else {
        continue;
      }

      if (innerCreator.isSuccess() || innerCreator.hasOnlyInvalidDescriptorErrors()) {
        if (result != null) {
          String firstDescriptor = result.getActualFile().getName();
          String secondDescriptor = innerCreator.getActualFile().getName();
          return new PluginCreator(descriptorPath, new MultiplePluginDescriptorsInLibDirectory(firstDescriptor, secondDescriptor), root);
        } else {
          result = innerCreator;
        }
      }
    }
    if (result != null) {
      return result;
    }
    return new PluginCreator(descriptorPath, new PluginDescriptorIsNotFound(descriptorPath), root);
  }

  private void sortFilesWithRespectToRootDirectoryName(@NotNull final File root, File[] files) {
    //move plugin-jar to the beginning: Sample.jar goes first (if Sample is a plugin name)
    final String rootDirectoryName = root.getName();
    Arrays.sort(files, new Comparator<File>() {
      @Override
      public int compare(@NotNull File o1, @NotNull File o2) {
        if (o2.getName().startsWith(rootDirectoryName)) return Integer.MAX_VALUE;
        if (o1.getName().startsWith(rootDirectoryName)) return -Integer.MAX_VALUE;
        if (o2.getName().startsWith("resources")) return -Integer.MAX_VALUE;
        if (o1.getName().startsWith("resources")) return Integer.MAX_VALUE;
        return 0;
      }
    });
  }

  @NotNull
  private List<URL> getInLibMetaInfUrls(File[] files) {
    List<URL> inLibJarUrls = new ArrayList<URL>();
    for (File file : files) {
      if (JarsUtils.isJarOrZip(file)) {
        try {
          String metaInfUrl = URLUtil.getJarEntryURL(file, "META-INF").toExternalForm();
          inLibJarUrls.add(new URL(metaInfUrl));
        } catch (Exception e) {
          LOG.warn("Unable to create URL for " + file + " META-INF root", e);
        }
      }
    }
    return inLibJarUrls;
  }

  @NotNull
  private PluginCreator loadDescriptorFromJarOrDirectory(@NotNull File jarOrDirectory,
                                                         @NotNull String descriptorPath,
                                                         boolean validateDescriptor) {
    descriptorPath = toSystemIndependentName(descriptorPath);
    PluginCreator pluginCreator;
    if (jarOrDirectory.isDirectory()) {
      pluginCreator = loadDescriptorFromDir(jarOrDirectory, descriptorPath, validateDescriptor);
    } else if (JarsUtils.isJar(jarOrDirectory)) {
      pluginCreator = loadDescriptorFromJarFile(jarOrDirectory, descriptorPath, myPathResolver, validateDescriptor);
    } else {
      return new PluginCreator(descriptorPath, new IncorrectPluginFile(jarOrDirectory), jarOrDirectory);
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
    if (configurationFile.startsWith("/META-INF/")) {
      configurationFile = StringUtil.trimStart(configurationFile, "/META-INF/");
    }
    return loadDescriptorFromJarOrDirectory(jarOrDirectory, configurationFile, false);
  }

  @NotNull
  private PluginCreator extractZipAndCreatePlugin(@NotNull File zipPlugin,
                                                  boolean readClassFiles,
                                                  boolean validateDescriptor) {
    ExtractorResult extractorResult;
    try {
      extractorResult = PluginExtractor.INSTANCE.extractPlugin(zipPlugin);
    } catch (Exception e) {
      return new PluginCreator(PLUGIN_XML, new UnableToExtractZip(zipPlugin), zipPlugin);
    }
    if (extractorResult instanceof ExtractorSuccess) {
      ExtractedPluginFile extractedPluginFile = ((ExtractorSuccess) extractorResult).getExtractedPlugin();
      return createPluginAndReadClasses(extractedPluginFile, readClassFiles, validateDescriptor);
    } else {
      return new PluginCreator(PLUGIN_XML, ((ExtractorFail) extractorResult).getPluginProblem(), zipPlugin);
    }
  }

  @NotNull
  private PluginCreator createPluginAndReadClasses(ExtractedPluginFile extractedPluginFile, boolean readClassFiles, boolean validateDescriptor) {
    try {
      PluginCreator pluginCreator = loadDescriptorFromJarOrDirectory(extractedPluginFile.getActualPluginFile(), PLUGIN_XML, validateDescriptor);
      if (readClassFiles) {
        pluginCreator.readClassFiles(extractedPluginFile);
      }
      return pluginCreator;
    } finally {
      if (!readClassFiles) {
        try {
          extractedPluginFile.close();
        } catch (IOException e) {
          LOG.warn("Unable to delete temporary extracted plugin file " + extractedPluginFile, e);
        }
      }
    }
  }

  @NotNull
  @Override
  public PluginCreationResult createPlugin(@NotNull File pluginFile, boolean readClassFiles, boolean validateDescriptor) {
    if (!pluginFile.exists()) {
      throw new IllegalArgumentException("Plugin file " + pluginFile + " does not exist");
    }
    PluginCreator pluginCreator;
    if (JarsUtils.isZip(pluginFile)) {
      pluginCreator = extractZipAndCreatePlugin(pluginFile, readClassFiles, validateDescriptor);
    } else {
      pluginCreator = loadDescriptorFromJarOrDirectory(pluginFile, PLUGIN_XML, validateDescriptor);
      if (readClassFiles) {
        pluginCreator.readClassFiles(new ExtractedPluginFile(pluginFile, null));
      }
    }
    pluginCreator.setOriginalFile(pluginFile);
    return pluginCreator.getPluginCreationResult();
  }

}
