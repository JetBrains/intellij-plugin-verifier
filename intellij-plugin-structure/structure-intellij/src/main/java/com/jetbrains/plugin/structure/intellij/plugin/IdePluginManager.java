package com.jetbrains.plugin.structure.intellij.plugin;

import com.jetbrains.plugin.structure.base.plugin.*;
import com.jetbrains.plugin.structure.base.problems.*;
import com.jetbrains.plugin.structure.base.utils.FileUtilKt;
import com.jetbrains.plugin.structure.intellij.extractor.ExtractedPlugin;
import com.jetbrains.plugin.structure.intellij.extractor.ExtractorResult;
import com.jetbrains.plugin.structure.intellij.extractor.PluginExtractor;
import com.jetbrains.plugin.structure.intellij.problems.PluginFileErrorsKt;
import com.jetbrains.plugin.structure.intellij.problems.PluginLibDirectoryIsEmpty;
import com.jetbrains.plugin.structure.intellij.problems.UnableToReadJarFile;
import com.jetbrains.plugin.structure.intellij.resources.CompositeResourceResolver;
import com.jetbrains.plugin.structure.intellij.resources.DefaultResourceResolver;
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver;
import com.jetbrains.plugin.structure.intellij.utils.JDOMUtil;
import com.jetbrains.plugin.structure.intellij.utils.URLUtil;
import com.jetbrains.plugin.structure.intellij.version.IdeVersion;
import kotlin.io.FilesKt;
import kotlin.text.StringsKt;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class IdePluginManager implements PluginManager<IdePlugin> {

  private static final Logger LOG = LoggerFactory.getLogger(IdePluginManager.class);

  public static final String PLUGIN_XML = "plugin.xml";
  public static final String META_INF = "META-INF";

  @NotNull
  private final ResourceResolver myResourceResolver;

  @NotNull
  private final File myExtractDirectory;

  private IdePluginManager(@NotNull ResourceResolver resourceResolver, @NotNull File extractDirectory) {
    myResourceResolver = resourceResolver;
    myExtractDirectory = extractDirectory;
  }

  @NotNull
  public static IdePluginManager createManager() {
    return createManager(DefaultResourceResolver.INSTANCE);
  }

  @NotNull
  public static IdePluginManager createManager(@NotNull File extractDirectory) {
    return createManager(DefaultResourceResolver.INSTANCE, extractDirectory);
  }

  @NotNull
  public static IdePluginManager createManager(@NotNull ResourceResolver resourceResolver) {
    return createManager(resourceResolver, Settings.EXTRACT_DIRECTORY.getAsFile());
  }

  @NotNull
  public static IdePluginManager createManager(@NotNull ResourceResolver resourceResolver, @NotNull File extractDirectory) {
    return new IdePluginManager(resourceResolver, extractDirectory);
  }

  private PluginCreator loadPluginInfoFromJarFile(@NotNull File jarFile,
                                                  @NotNull final String descriptorPath,
                                                  boolean validateDescriptor,
                                                  @NotNull final ResourceResolver resourceResolver) {
    ZipFile zipFile;
    try {
      zipFile = new ZipFile(jarFile);
    } catch (Exception e) {
      LOG.info("Unable to read jar file " + jarFile, e);
      return PluginCreator.createInvalidPlugin(jarFile, descriptorPath, new UnableToReadJarFile());
    }

    try {
      String entryName = META_INF + "/" + descriptorPath;
      ZipEntry entry = getZipEntry(zipFile, toCanonicalPath(entryName));
      if (entry != null) {
        try (InputStream documentStream = zipFile.getInputStream(entry)) {
          Document document = JDOMUtil.loadDocument(documentStream);
          List<PluginIcon> icons = getIconsFromJarFile(zipFile);
          URL documentUrl = URLUtil.getJarEntryURL(jarFile, entry.getName());
          PluginCreator plugin = PluginCreator.createPlugin(jarFile, descriptorPath, validateDescriptor, document, documentUrl, resourceResolver);
          plugin.setIcons(icons);
          return plugin;
        } catch (Exception e) {
          LOG.info("Unable to read file " + descriptorPath, e);
          String message = e.getLocalizedMessage();
          return PluginCreator.createInvalidPlugin(jarFile, descriptorPath, new UnableToReadDescriptor(descriptorPath, message));
        }
      } else {
        return PluginCreator.createInvalidPlugin(jarFile, descriptorPath, new PluginDescriptorIsNotFound(descriptorPath));
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
  private List<PluginIcon> getIconsFromJarFile(@NotNull ZipFile jarFile) throws IOException {
    List<PluginIcon> icons = new ArrayList<>();
    for (IconTheme theme : IconTheme.values()) {
      String iconEntryName = META_INF + "/" + getIconFileName(theme);
      ZipEntry entry = getZipEntry(jarFile, toCanonicalPath(iconEntryName));
      if (entry == null) {
        continue;
      }
      byte[] iconContent = new byte[(int) entry.getSize()];
      IOUtils.readFully(jarFile.getInputStream(entry), iconContent);
      icons.add(new PluginIcon(theme, iconContent, iconEntryName));
    }
    return icons;
  }

  private PluginCreator loadPluginInfoFromDirectory(@NotNull File pluginDirectory,
                                                    @NotNull String descriptorPath,
                                                    boolean validateDescriptor,
                                                    @NotNull ResourceResolver resourceResolver) {
    File descriptorFile = new File(new File(pluginDirectory, META_INF), FileUtilKt.toSystemIndependentName(descriptorPath));
    if (!descriptorFile.exists()) {
      return loadPluginInfoFromLibDirectory(pluginDirectory, descriptorPath, validateDescriptor, resourceResolver);
    }

    try {
      URL documentUrl = URLUtil.fileToUrl(descriptorFile);
      Document document = JDOMUtil.loadDocument(documentUrl);
      List<PluginIcon> icons = loadIconsFromDir(pluginDirectory);
      PluginCreator plugin = PluginCreator.createPlugin(pluginDirectory, descriptorPath, validateDescriptor, document, documentUrl, resourceResolver);
      plugin.setIcons(icons);
      return plugin;
    } catch (JDOMParseException e) {
      int lineNumber = e.getLineNumber();
      String message = lineNumber != -1 ? "unexpected element on line " + lineNumber : "unexpected elements";
      LOG.info("Unable to parse plugin descriptor " + descriptorPath + " of plugin " + descriptorFile, e);
      return PluginCreator.createInvalidPlugin(pluginDirectory, descriptorPath, new UnexpectedDescriptorElements(message, descriptorPath));
    } catch (Exception e) {
      LOG.info("Unable to read plugin descriptor " + descriptorPath + " of plugin " + descriptorFile, e);
      return PluginCreator.createInvalidPlugin(pluginDirectory, descriptorPath, new UnableToReadDescriptor(descriptorPath, descriptorPath));
    }
  }

  private List<PluginIcon> loadIconsFromDir(@NotNull File pluginDirectory) throws IOException {
    List<PluginIcon> icons = new ArrayList<>();
    for (IconTheme theme : IconTheme.values()) {
      File iconFile = new File(new File(pluginDirectory, META_INF), FileUtilKt.toSystemIndependentName(getIconFileName(theme)));
      if (!iconFile.exists()) {
        continue;
      }
      byte[] iconContent = new byte[(int) iconFile.length()];
      IOUtils.readFully(new FileInputStream(iconFile), iconContent);
      icons.add(new PluginIcon(theme, iconContent, iconFile.getName()));
    }
    return icons;
  }

  private PluginCreator loadPluginInfoFromLibDirectory(@NotNull final File root,
                                                       @NotNull String descriptorPath,
                                                       boolean validateDescriptor,
                                                       @NotNull ResourceResolver resourceResolver) {
    File libDir = new File(root, "lib");
    if (!libDir.isDirectory()) {
      return PluginCreator.createInvalidPlugin(root, descriptorPath, new PluginDescriptorIsNotFound(descriptorPath));
    }

    final File[] files = libDir.listFiles();
    if (files == null || files.length == 0) {
      return PluginCreator.createInvalidPlugin(root, descriptorPath, new PluginLibDirectoryIsEmpty());
    }
    putMoreLikelyPluginJarsFirst(root, files);

    List<File> jarFiles = Arrays.stream(files).filter(FileUtilKt::isJar).collect(Collectors.toList());
    ResourceResolver libResourceResolver = new JarFilesResourceResolver(jarFiles);
    ResourceResolver compositeResolver = new CompositeResourceResolver(Arrays.asList(libResourceResolver, resourceResolver));

    List<PluginCreator> results = new ArrayList<>();

    for (File file : files) {
      PluginCreator innerCreator;
      if (FileUtilKt.isJar(file) || FileUtilKt.isZip(file)) {
        //Use the composite resource resolver, which can resolve resources in lib's jar files.
        innerCreator = loadPluginInfoFromJarFile(file, descriptorPath, validateDescriptor, compositeResolver);
      } else if (file.isDirectory()) {
        //Use the common resource resolver, which is unaware of lib's jar files.
        innerCreator = loadPluginInfoFromDirectory(file, descriptorPath, validateDescriptor, resourceResolver);
      } else {
        continue;
      }

      results.add(innerCreator);
    }

    List<PluginCreator> possibleResults = results.stream()
        .filter(r -> r.isSuccess() || hasOnlyInvalidDescriptorErrors(r))
        .collect(Collectors.toList());

    if (possibleResults.size() > 1) {
      PluginCreator first = possibleResults.get(0);
      PluginCreator second = possibleResults.get(1);
      PluginProblem multipleDescriptorsProblem = new MultiplePluginDescriptors(
          first.getDescriptorPath(),
          first.getPluginFile().getName(),
          second.getDescriptorPath(),
          second.getPluginFile().getName()
      );
      return PluginCreator.createInvalidPlugin(root, descriptorPath, multipleDescriptorsProblem);
    }

    if (possibleResults.size() == 1) {
      return possibleResults.get(0);
    }

    return PluginCreator.createInvalidPlugin(root, descriptorPath, new PluginDescriptorIsNotFound(descriptorPath));
  }

  private static boolean hasOnlyInvalidDescriptorErrors(@NotNull PluginCreator creator) {
    PluginCreationResult<IdePlugin> pluginCreationResult = creator.getPluginCreationResult();
    if (pluginCreationResult instanceof PluginCreationSuccess) {
      return false;
    }
    List<PluginProblem> errorsAndWarnings = ((PluginCreationFail<IdePlugin>) pluginCreationResult).getErrorsAndWarnings();
    return errorsAndWarnings.stream().allMatch(p -> p.getLevel() != PluginProblem.Level.ERROR || p instanceof InvalidDescriptorProblem);
  }

  /*
   * Sort the files heuristically to load the plugin jar containing plugin descriptors without extra ZipFile accesses
   * File name preference:
   * a) last order for files with resources in name, like resources_en.jar
   * b) last order for files that have -digit suffix is the name e.g. completion-ranking.jar is before json-2.8.0.jar or junit-m5.jar
   * c) jar with name close to plugin's directory name, e.g. kotlin-XXX.jar is before all-open-XXX.jar
   * d) shorter name, e.g. android.jar is before android-base-common.jar
   */
  private static void putMoreLikelyPluginJarsFirst(File pluginDir, File[] filesInLibUnderPluginDir) {
    String pluginDirName = pluginDir.getName();

    Arrays.parallelSort(filesInLibUnderPluginDir, (o1, o2) -> {
      String o2Name = o2.getName();
      String o1Name = o1.getName();

      boolean o2StartsWithResources = o2Name.startsWith("resources");
      boolean o1StartsWithResources = o1Name.startsWith("resources");
      if (o2StartsWithResources != o1StartsWithResources) {
        return o2StartsWithResources ? -1 : 1;
      }

      boolean o2IsVersioned = fileNameIsLikeVersionedLibraryName(o2Name);
      boolean o1IsVersioned = fileNameIsLikeVersionedLibraryName(o1Name);
      if (o2IsVersioned != o1IsVersioned) {
        return o2IsVersioned ? -1 : 1;
      }

      boolean o2StartsWithNeededName = StringsKt.startsWith(o2Name, pluginDirName, true);
      boolean o1StartsWithNeededName = StringsKt.startsWith(o1Name, pluginDirName, true);
      if (o2StartsWithNeededName != o1StartsWithNeededName) {
        return o2StartsWithNeededName ? 1 : -1;
      }

      return o1Name.length() - o2Name.length();
    });
  }

  private static boolean fileNameIsLikeVersionedLibraryName(String name) {
    int i = name.lastIndexOf('-');
    if (i == -1) return false;
    if (i + 1 < name.length()) {
      char c = name.charAt(i + 1);
      if (Character.isDigit(c)) return true;
      return (c == 'm' || c == 'M') && i + 2 < name.length() && Character.isDigit(name.charAt(i + 2));
    }
    return false;
  }

  @NotNull
  private PluginCreator loadPluginInfoFromJarOrDirectory(@NotNull File pluginFile,
                                                         @NotNull String descriptorPath,
                                                         boolean validateDescriptor,
                                                         ResourceResolver resourceResolver) {
    descriptorPath = FileUtilKt.toSystemIndependentName(descriptorPath);
    if (pluginFile.isDirectory()) {
      return loadPluginInfoFromDirectory(pluginFile, descriptorPath, validateDescriptor, resourceResolver);
    } else if (FileUtilKt.isJar(pluginFile)) {
      return loadPluginInfoFromJarFile(pluginFile, descriptorPath, validateDescriptor, resourceResolver);
    } else {
      throw new IllegalArgumentException();
    }
  }

  @NotNull
  private static String toCanonicalPath(@NotNull String descriptorPath) {
    return FilesKt.normalize(new File(FileUtilKt.toSystemIndependentName(descriptorPath))).getPath();
  }

  @Nullable
  private static ZipEntry getZipEntry(@NotNull ZipFile zipFile, @NotNull String entryPath) {
    String independentPath = FileUtilKt.toSystemIndependentName(entryPath);
    ZipEntry independentEntry = zipFile.getEntry(independentPath);
    if (independentEntry != null) {
      return independentEntry;
    }

    String dependentPath = entryPath.replace('/', File.separatorChar);
    if (!dependentPath.equals(independentPath)) {
      return zipFile.getEntry(dependentPath);
    }
    return null;
  }

  private void resolveOptionalDependencies(File pluginFile, PluginCreator pluginCreator, ResourceResolver resourceResolver) {
    if (pluginCreator.isSuccess()) {
      resolveOptionalDependencies(pluginCreator, new HashSet<>(), new LinkedList<>(), pluginFile, resourceResolver, pluginCreator);
    }
  }

  private void resolveOptionalDependencies(PluginCreator currentPlugin,
                                           Set<String> visitedConfigurationFiles,
                                           LinkedList<String> path,
                                           File pluginFile,
                                           ResourceResolver resourceResolver,
                                           PluginCreator mainPlugin) {
    if (!visitedConfigurationFiles.add(currentPlugin.getDescriptorPath())) {
      return;
    }
    path.addLast(currentPlugin.getDescriptorPath());

    Map<PluginDependency, String> optionalDependenciesConfigFiles = currentPlugin.getOptionalDependenciesConfigFiles();
    for (Map.Entry<PluginDependency, String> entry : optionalDependenciesConfigFiles.entrySet()) {
      PluginDependency pluginDependency = entry.getKey();
      String configurationFile = entry.getValue();

      if (path.contains(configurationFile)) {
        List<String> configurationFilesCycle = new ArrayList<>(path);
        configurationFilesCycle.add(configurationFile);
        mainPlugin.registerOptionalDependenciesConfigurationFilesCycleProblem(configurationFilesCycle);
        return;
      }

      PluginCreator optionalDependencyCreator = loadPluginInfoFromJarOrDirectory(pluginFile, configurationFile, false, resourceResolver);
      currentPlugin.addOptionalDescriptor(pluginDependency, configurationFile, optionalDependencyCreator);

      resolveOptionalDependencies(optionalDependencyCreator, visitedConfigurationFiles, path, pluginFile, resourceResolver, mainPlugin);
    }

    path.removeLast();
  }

  @NotNull
  private PluginCreator extractZipAndCreatePlugin(@NotNull File zipPlugin,
                                                  @NotNull String descriptorPath,
                                                  boolean validateDescriptor,
                                                  @NotNull ResourceResolver resourceResolver) {
    ExtractorResult extractorResult;
    try {
      extractorResult = PluginExtractor.INSTANCE.extractPlugin(zipPlugin, myExtractDirectory);
    } catch (Exception e) {
      LOG.info("Unable to extract plugin zip " + zipPlugin, e);
      return PluginCreator.createInvalidPlugin(zipPlugin, descriptorPath, new UnableToExtractZip());
    }
    if (extractorResult instanceof ExtractorResult.Success) {
      try (ExtractedPlugin extractedPlugin = ((ExtractorResult.Success) extractorResult).getExtractedPlugin()) {
        File extractedFile = extractedPlugin.getPluginFile();
        if (FileUtilKt.isJar(extractedFile) || extractedFile.isDirectory()) {
          PluginCreator pluginCreator = loadPluginInfoFromJarOrDirectory(extractedFile, descriptorPath, validateDescriptor, resourceResolver);
          resolveOptionalDependencies(extractedFile, pluginCreator, myResourceResolver);
          return pluginCreator;
        }

        return getInvalidPluginFileCreator(zipPlugin, descriptorPath);
      }
    } else {
      return PluginCreator.createInvalidPlugin(zipPlugin, descriptorPath, ((ExtractorResult.Fail) extractorResult).getPluginProblem());
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
    return pluginCreator.getPluginCreationResult();
  }

  public PluginCreationResult<IdePlugin> createBundledPlugin(@NotNull File pluginFile,
                                                             @NotNull IdeVersion ideVersion,
                                                             @NotNull String descriptorPath) {
    PluginCreator pluginCreator = getPluginCreatorWithResult(pluginFile, false, descriptorPath);
    pluginCreator.setPluginVersion(ideVersion.asStringWithoutProductCode());
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
      pluginCreator = extractZipAndCreatePlugin(pluginFile, descriptorPath, validateDescriptor, myResourceResolver);
    } else if (FileUtilKt.isJar(pluginFile) || pluginFile.isDirectory()) {
      pluginCreator = loadPluginInfoFromJarOrDirectory(pluginFile, descriptorPath, validateDescriptor, myResourceResolver);
      resolveOptionalDependencies(pluginFile, pluginCreator, myResourceResolver);
    } else {
      pluginCreator = getInvalidPluginFileCreator(pluginFile, descriptorPath);
    }
    pluginCreator.setOriginalFile(pluginFile);
    return pluginCreator;
  }

  @NotNull
  private PluginCreator getInvalidPluginFileCreator(@NotNull File pluginFile, @NotNull String descriptorPath) {
    return PluginCreator.createInvalidPlugin(pluginFile, descriptorPath, PluginFileErrorsKt.createIncorrectIntellijFileProblem(pluginFile.getName()));
  }

  @NotNull
  private static String getIconFileName(@NotNull IconTheme iconTheme) {
    return "pluginIcon" + iconTheme.getSuffix() + ".svg";
  }

}
