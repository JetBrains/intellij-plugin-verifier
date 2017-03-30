package com.intellij.structure.impl.domain;

import com.google.common.base.Supplier;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginDependency;
import com.intellij.structure.domain.PluginManager;
import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.impl.utils.Pair;
import com.intellij.structure.impl.utils.Ref;
import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.impl.utils.validators.PluginXmlValidator;
import com.intellij.structure.impl.utils.validators.Validator;
import com.intellij.structure.impl.utils.xml.JDOMUtil;
import com.intellij.structure.impl.utils.xml.JDOMXIncluder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static com.intellij.structure.impl.utils.StringUtil.substringAfter;
import static com.intellij.structure.impl.utils.StringUtil.toSystemIndependentName;

/**
 * @author Sergey Patrikeev
 */
public class PluginManagerImpl extends PluginManager {

  private static final String PLUGIN_XML = "plugin.xml";
  private static final Pattern LIB_JAR_REGEX = Pattern.compile("([^/]+/)?lib/([^/]+\\.(jar|zip))");

  private static final Logger LOG = LoggerFactory.getLogger(PluginManagerImpl.class);

  private static final Pattern XML_IN_META_INF_PATTERN = Pattern.compile("([^/]*/)?META-INF/(([^/]+/)*(\\w|\\-)+\\.xml)");

  /**
   * <p>Contains all the .xml files under META-INF/ directory and subdirectories.</p> It consists of the following
   * entries: (file path relative to META-INF/ dir) TO pair of (full URL path of the file) and (corresponding Document)
   * <p>It will be used later to resolve optional descriptors.</p>
   */
  private final Map<String, Pair<URL, Document>> myRootXmlDocuments = new HashMap<String, Pair<URL, Document>>();

  private final List<String> myHints = new ArrayList<String>();

  private File myPluginFile;

  public static boolean isJarOrZip(@NotNull File file) {
    if (file.isDirectory()) {
      return false;
    }
    final String name = file.getName();
    return StringUtil.endsWithIgnoreCase(name, ".jar") || StringUtil.endsWithIgnoreCase(name, ".zip");
  }

  @NotNull
  public static String getFileEscapedUri(@NotNull File file) {
    return StringUtil.replace(file.toURI().toASCIIString(), "!", "%21");
  }

  @NotNull
  private static String getJarRootUrl(@NotNull File jarFile) {
    return "jar:" + getFileEscapedUri(jarFile) + "!/";
  }

  @NotNull
  private static String getMissingDepMsg(String dependencyId, String configFile) {
    return "Plugin dependency " + dependencyId + " config-file " + configFile + " specified in META-INF/plugin.xml is not found";
  }

  /**
   * <p>Searches the descriptor on the {@code filePath} relative to META-INF/ directory.</p>
   * Example:
   * <p>If {@code filePath} == plugin.xml => loads ..META-INF/plugin.xml</p>
   * <p>If {@code filePath} == relative/plugin.xml => loads ..META-INF/relative/plugin.xml</p>
   * <p>If {@code filePath} == ../brotherDir/optional.xml => loads ..META-INF/../brotherDir/plugin.xml</p>
   * and so on...
   *
   * @param file      plugin file
   * @param filePath  descriptor file path relative to META-INF/ directory
   * @param validator problems controller
   * @return plugin descriptor
   * @throws IncorrectPluginException if plugin is broken
   */
  @Nullable
  private PluginBuilder loadDescriptor(@NotNull final File file, @NotNull String filePath, @NotNull Validator validator) throws IncorrectPluginException {
    filePath = toSystemIndependentName(filePath);

    PluginBuilder builder;

    if (file.isDirectory()) {
      builder = loadDescriptorFromDir(file, filePath, validator);
    } else if (file.exists() && isJarOrZip(file)) {
      builder = loadDescriptorFromZipOrJarFile(file, filePath, validator, PluginImpl.DEFAULT_PLUGIN_XML_PATH_RESOLVER);
    } else {
      if (!file.exists()) {
        validator.onIncorrectStructure("Plugin file is not found " + file);
      } else {
        validator.onIncorrectStructure("Incorrect plugin file type " + file + ". Should be a .zip or .jar archive or a directory.");
      }
      return null;
    }

    if (builder != null) {
      resolveOptionalDescriptors(file, filePath, builder, validator);
    }

    if (builder == null) {
      validator.onMissingFile("META-INF/" + filePath + " is not found");
    }

    return builder;
  }

  private void resolveOptionalDescriptors(@NotNull File file,
                                          @NotNull String filePath,
                                          @NotNull PluginBuilder descriptor,
                                          @NotNull Validator parentValidator) throws IncorrectPluginException {
    Map<PluginDependency, String> optionalConfigs = descriptor.getOptionalConfigFiles();
    if (!optionalConfigs.isEmpty()) {
      Map<String, Plugin> descriptors = new HashMap<String, Plugin>();

      List<URL> metaInfBases = getOptionalMetaInfBases();
      JDOMXIncluder.PathResolver pathResolver = new PluginImpl.PluginXmlPathResolver(metaInfBases);

      for (Map.Entry<PluginDependency, String> entry : optionalConfigs.entrySet()) {
        String optFilePath = entry.getValue();

        if (StringUtil.equal(filePath, optFilePath)) {
          parentValidator.onIncorrectStructure("Plugin has recursive config dependencies for descriptor " + filePath);
        }

        final String original = optFilePath;
        if (optFilePath.startsWith("/META-INF/")) {
          optFilePath = StringUtil.trimStart(optFilePath, "/META-INF/");
        }

        Pair<URL, Document> xmlPair = myRootXmlDocuments.get(optFilePath);
        if (xmlPair != null) {
          URL url = xmlPair.getFirst();
          if (url != null) {
            Document document = xmlPair.getSecond();
            PluginBuilder optDescriptor = new PluginBuilder(myPluginFile);
            try {
              PluginInfoExtractor extractor = new PluginInfoExtractor(optDescriptor, parentValidator.ignoreMissingConfigElement());
              extractor.readExternal(document, url, pathResolver);
              descriptors.put(original, optDescriptor.build());
            } catch (Exception e) {
              String msg = getMissingDepMsg(entry.getKey().getId(), entry.getValue());
              myHints.add(msg);
              LOG.debug(msg, e);
            }
          }
        } else {
          //don't complain if the file is not found and don't complain if it has incorrect .xml structure
          Validator optValidator = parentValidator.ignoreMissingConfigElement().ignoreMissingFile();

          try {
            PluginBuilder optDescriptor = loadDescriptor(file, optFilePath, optValidator);
            if (optDescriptor == null) {
              String msg = getMissingDepMsg(entry.getKey().getId(), entry.getValue());
              myHints.add(msg);
              LOG.debug(msg);
            } else {
              descriptors.put(original, optDescriptor.build());
            }
          } catch (Exception e) {
            String msg = getMissingDepMsg(entry.getKey().getId(), entry.getValue());
            myHints.add(msg);
            LOG.debug(msg, e);
          }

        }
      }

      descriptor.setOptionalDescriptors(descriptors);
    }
  }

  @NotNull
  private List<URL> getOptionalMetaInfBases() {
    List<URL> metaInfBases = new ArrayList<URL>();
    for (Pair<URL, Document> pair : myRootXmlDocuments.values()) {
      String url = StringUtil.substringBeforeIncluding(pair.getFirst().toExternalForm(), "META-INF/");
      if (url != null) {
        try {
          metaInfBases.add(new URL(url));
        } catch (MalformedURLException e) {
          LOG.warn("Unable to create URL by " + url, e);
        }
      }
    }
    return metaInfBases;
  }

  //filePath is relative to META-INF/ => should resolve it properly

  /**
   * Checks that the given {@code entry} corresponds to the sought-for file specified with {@code filePath}.
   *
   * @param entry               current entry in the overlying traversing of zip file
   * @param filePath            sought-for file, path is relative to META-INF/ directory
   * @param rootUrl             url corresponding to the root of the zip file from which this {@code entry} come
   * @param validator           problems resolver
   * @param pathResolver        resolver of the relative paths (for xinclude elements)
   * @param entryStreamSupplier supplies the input stream for this entry if needed
   * @return sought-for descriptor or null
   * @throws IncorrectPluginException if incorrect plugin structure
   */
  @Nullable
  private PluginBuilder loadDescriptorFromEntry(@NotNull ZipEntry entry,
                                         @NotNull String filePath,
                                         @NotNull String rootUrl,
                                         @NotNull Validator validator,
                                         @NotNull JDOMXIncluder.PathResolver pathResolver,
                                         @NotNull Supplier<InputStream> entryStreamSupplier) throws IncorrectPluginException {
    Matcher xmlMatcher = XML_IN_META_INF_PATTERN.matcher(entry.getName());
    if (xmlMatcher.matches()) {
      final String xmlUrl = rootUrl + entry.getName();
      String name = xmlMatcher.group(2);

      Document document;
      URL url;
      try {
        //get input stream for this entry
        InputStream stream = entryStreamSupplier.get();
        if (stream == null) {
          return null;
        }
        document = JDOMUtil.loadDocument(stream);
        url = new URL(xmlUrl);
      } catch (Exception e) {
        //check if an exception happened on the sought-for entity
        if (StringUtil.equal(name, filePath)) {
          validator.onCheckedException("Unable to read META-INF/" + name, e);
        }
        LOG.warn("Unable to read an entry " + entry.getName(), e);
        return null;
      }

      if (StringUtil.equal(name, filePath)) {
        PluginBuilder builder = new PluginBuilder(myPluginFile);
        PluginInfoExtractor extractor = new PluginInfoExtractor(builder, validator);
        extractor.readExternal(document, url, pathResolver);
        return builder;
      } else {
        //add this .xml for the future check
        Pair<URL, Document> pair = Pair.create(url, document);
        myRootXmlDocuments.put(name, pair);
      }
    } else if (filePath.startsWith("../")) {
      //for example filePath == ../brotherDir/opt.xml
      // => absolute path == <in_zip_path>/META-INF/../brotherDir/opt.xml
      //                  == <in_zip_path>/brotherDir/opt.xml
      filePath = StringUtil.trimStart(filePath, "../");
      if (filePath.startsWith("../")) {
        //we don't support ../../opts/opt.xml paths yet (is it needed?)
        return null;
      }
      if (entry.getName().endsWith(filePath)) {
        //this xml is probably what is searched for

        InputStream is = entryStreamSupplier.get();
        if (is == null) {
          return null;
        }
        try {
          Document document = JDOMUtil.loadDocument(is);
          String xmlUrl = rootUrl + entry.getName();
          URL url = new URL(xmlUrl);

          PluginBuilder builder = new PluginBuilder(myPluginFile);
          PluginInfoExtractor extractor = new PluginInfoExtractor(builder, validator);
          extractor.readExternal(document, url, pathResolver);
          return builder;
        } catch (RuntimeException e) {
          //rethrow a RuntimeException but wrap a checked exception
          throw e;
        } catch (Exception e) {
          validator.onCheckedException("Unable to read META-INF/" + filePath, e);
          return null;
        }
      }
    }
    return null;
  }

  @Nullable
  private PluginBuilder loadFromZipStream(@NotNull final ZipInputStream zipStream,
                                   @NotNull String zipRootUrl,
                                   @NotNull String filePath,
                                   @NotNull Validator validator,
                                   @NotNull JDOMXIncluder.PathResolver pathResolver) throws IncorrectPluginException {
    PluginBuilder descriptor = null;

    try {
      ZipEntry entry;
      while ((entry = zipStream.getNextEntry()) != null) {
        if (entry.isDirectory()) {
          continue;
        }

        PluginBuilder inRoot = loadDescriptorFromEntry(entry, filePath, zipRootUrl, validator, pathResolver, new Supplier<InputStream>() {
          @Override
          public InputStream get() {
            return zipStream;
          }
        });

        if (inRoot != null) {
          if (descriptor != null) {
            validator.onIncorrectStructure("Multiple META-INF/" + filePath + " found");
            return null;
          }
          descriptor = inRoot;
        }
      }
    } catch (IOException e) {
      validator.onCheckedException("Unable to load META-INF/" + filePath, e);
      return null;
    }

    if (descriptor == null) {
      validator.onMissingFile("META-INF/" + filePath + " is not found");
    }

    return descriptor;
  }

  /**
   * Given a .zip or .jar file searches the file on {@code filePath} relative to META-INF/ directory.
   * The given .zip file could be a compressed plugin directory, a compressed .jar file or just a single .jar file.
   *
   * @param file         zip or jar file
   * @param filePath     file path of the .xml relative to META-INF/ directory
   * @param validator    plugin problems validator
   * @param pathResolver used for resolving xinclude documents
   * @return plugin or null if failed
   * @throws IncorrectPluginException in case of incorrect plugin structure or missing config elements
   */
  @Nullable
  private PluginBuilder loadDescriptorFromZipOrJarFile(@NotNull final File file,
                                                @NotNull final String filePath,
                                                @NotNull final Validator validator,
                                                @NotNull final JDOMXIncluder.PathResolver pathResolver) throws IncorrectPluginException {
    final String zipRootUrl = getJarRootUrl(file);

    PluginBuilder descriptorRoot = null;
    PluginBuilder descriptorInner = null;

    final ZipFile zipFile;
    try {
      zipFile = new ZipFile(file);
    } catch (IOException e) {
      validator.onCheckedException("Unable to read plugin file " + file, e);
      return null;
    }

    try {
      List<? extends ZipEntry> entries = Collections.list(zipFile.entries());

      List<URL> inLibMetaInfUrls = getInLibMetaInfUrls(zipRootUrl, entries);
      JDOMXIncluder.PathResolver innerLibJarsResolver = new PluginImpl.PluginXmlPathResolver(inLibMetaInfUrls);

      for (final ZipEntry entry : entries) {
        if (entry.isDirectory()) {
          continue;
        }

        //check if this .zip file is an archive of the .jar file
        //in this case it contains the following entry: a.zip!/b.jar!/META-INF/plugin.xml
        if (entry.getName().indexOf('/') == -1 && entry.getName().endsWith(".jar")) {
          //this is in-root .jar file which will be extracted by the IDE
          ZipInputStream inRootJar = new ZipInputStream(zipFile.getInputStream(entry));
          String inRootJarUrl = getInnerJarRootUrl(zipRootUrl, entry);
          PluginBuilder plugin = loadFromZipStream(inRootJar, inRootJarUrl, filePath, validator.ignoreMissingFile(), pathResolver);
          if (plugin != null) {
            if (descriptorRoot != null) {
              validator.onIncorrectStructure("Multiple META-INF/" + filePath + " found in the root of the plugin");
              return null;
            }
            descriptorRoot = plugin;
          }
        }

        final Ref<IOException> maybeException = Ref.create();
        PluginBuilder inRoot = loadDescriptorFromEntry(entry, filePath, zipRootUrl, validator.ignoreMissingFile(), pathResolver, new Supplier<InputStream>() {
          @Override
          public InputStream get() {
            try {
              return zipFile.getInputStream(entry);
            } catch (IOException e) {
              maybeException.set(e);
              return null;
            }
          }
        });
        if (!maybeException.isNull()) {
          throw maybeException.get();
        }

        if (inRoot != null) {
          if (descriptorRoot != null) {
            String msg = "Multiple META-INF/" + filePath + " found in the root of the plugin";
            LOG.warn(msg);
            myHints.add(msg);
          }
          descriptorRoot = inRoot;
          continue;
        }

        if (LIB_JAR_REGEX.matcher(entry.getName()).matches()) {
          ZipInputStream inner = new ZipInputStream(zipFile.getInputStream(entry));
          PluginBuilder innerDescriptor = loadFromZipStream(inner, "jar:" + zipRootUrl + entry.getName() + "!/", filePath, validator.ignoreMissingFile(), innerLibJarsResolver);
          if (innerDescriptor != null) {
            descriptorInner = innerDescriptor;
          }
        }
      }
    } catch (IOException e) {
      validator.onCheckedException("Unable to read plugin file " + file, e);
      return null;
    } finally {
      try {
        zipFile.close();
      } catch (IOException ignored) {
      }
    }

    //in-root descriptor takes precedence over other descriptors, so don't throw
    //"Multiple plugin.xml" if they are found in the <root>/META-INF/plugin.xml and <root>/lib/some.jar/META-INF/plugin.xml
    if (descriptorRoot != null) {

      if (descriptorInner != null) {
        //some plugins have logo-file in the lib-descriptor
        if (descriptorInner.getMyLogoContent() != null) {
          descriptorRoot.setLogoContent(descriptorInner.getMyLogoContent());
        }
      }

      return descriptorRoot;
    }

    if (descriptorInner != null) {
      return descriptorInner;
    }

    validator.onMissingFile("META-INF/" + filePath + " is not found");
    return null;
  }

  @NotNull
  private String getInnerJarRootUrl(@NotNull String zipRootUrl, @NotNull ZipEntry entry) {
    return "jar:" + zipRootUrl + StringUtil.trimStart(entry.getName(), "/") + "!/";
  }

  @NotNull
  private List<URL> getInLibMetaInfUrls(String zipRootUrl, List<? extends ZipEntry> entries) {
    List<URL> result = new ArrayList<URL>();
    for (ZipEntry entry : entries) {
      if (LIB_JAR_REGEX.matcher(entry.getName()).matches()) {
        String metaInfUrl = getInnerJarRootUrl(zipRootUrl, entry) + "META-INF/";
        try {
          URL url = new URL(metaInfUrl);
          result.add(url);
        } catch (MalformedURLException e) {
          LOG.warn("Unable to create url by " + metaInfUrl, e);
        }
      }
    }
    return result;
  }


  @Nullable
  private PluginBuilder loadDescriptorFromDir(@NotNull final File dir, @NotNull String filePath, @NotNull Validator validator) throws IncorrectPluginException {
    File descriptorFile = new File(dir, "META-INF" + File.separator + StringUtil.toSystemIndependentName(filePath));
    if (descriptorFile.exists()) {
      return loadDescriptorFromDirRoot(dir, filePath, validator, descriptorFile);
    }
    return loadDescriptorFromLibDir(dir, filePath, validator);
  }

  @Nullable
  private PluginBuilder loadDescriptorFromDirRoot(@NotNull File dir, @NotNull String filePath, @NotNull Validator validator, File descriptorFile) {
    appendXmlInRoot(filePath, validator, descriptorFile);

    PluginBuilder builder = new PluginBuilder(myPluginFile);
    PluginInfoExtractor extractor = new PluginInfoExtractor(builder, validator);

    URL url;
    try {
      url = descriptorFile.toURI().toURL();
    } catch (MalformedURLException e) {
      validator.onCheckedException("File " + dir + " contains invalid plugin descriptor " + filePath, e);
      return null;
    }

    Document document = readDocument(url, validator);
    if (document == null) {
      return null;
    }

    extractor.readExternal(document, url, PluginImpl.DEFAULT_PLUGIN_XML_PATH_RESOLVER);

    return builder;
  }

  private void appendXmlInRoot(@NotNull String filePath, @NotNull Validator validator, @NotNull File descriptorFile) {
    Collection<File> allXmlUnderMetaInf = FileUtils.listFiles(descriptorFile.getParentFile(), new String[]{"xml"}, true);
    for (File xml : allXmlUnderMetaInf) {
      InputStream inputStream = null;
      try {
        inputStream = FileUtils.openInputStream(xml);
        Document document = JDOMUtil.loadDocument(inputStream);
        String relativePath = substringAfter(toSystemIndependentName(xml.getAbsolutePath()), "META-INF/");
        myRootXmlDocuments.put(relativePath, Pair.create(xml.toURI().toURL(), document));
      } catch (Exception e) {
        if (StringUtil.equal(xml.getName(), StringUtil.getFileName(filePath))) {
          validator.onCheckedException("Unable to read .xml file META-INF/" + filePath, e);
        }
      } finally {
        IOUtils.closeQuietly(inputStream);
      }
    }
  }

  @Nullable
  private Document readDocument(@NotNull URL url, @NotNull Validator validator) {
    try {
      return JDOMUtil.loadDocument(url);
    } catch (JDOMException e) {
      validator.onCheckedException("Unable to parse xml file " + url, e);
    } catch (IOException e) {
      validator.onCheckedException("Unable to read xml file " + url, e);
    }
    return null;
  }

  @Nullable
  private PluginBuilder loadDescriptorFromLibDir(@NotNull final File dir, @NotNull String filePath, @NotNull Validator validator) throws IncorrectPluginException {
    File libDir = new File(dir, "lib");
    if (!libDir.isDirectory()) {
      validator.onMissingFile("Plugin `lib` directory is not found");
      return null;
    }

    final File[] files = libDir.listFiles();
    if (files == null || files.length == 0) {
      validator.onIncorrectStructure("Plugin `lib` directory " + libDir + " is empty");
      return null;
    }
    //move plugin-jar to the beginning: Sample.jar goes first (if Sample is a plugin name)
    Arrays.sort(files, new Comparator<File>() {
      @Override
      public int compare(@NotNull File o1, @NotNull File o2) {
        if (o2.getName().startsWith(dir.getName())) return Integer.MAX_VALUE;
        if (o1.getName().startsWith(dir.getName())) return -Integer.MAX_VALUE;
        if (o2.getName().startsWith("resources")) return -Integer.MAX_VALUE;
        if (o1.getName().startsWith("resources")) return Integer.MAX_VALUE;
        return 0;
      }
    });

    PluginBuilder descriptor = null;

    List<URL> metaInfUrls = getInLibMetaInfUrls(files);
    JDOMXIncluder.PathResolver pathResolver = new PluginImpl.PluginXmlPathResolver(metaInfUrls);

    for (final File f : files) {
      if (isJarOrZip(f)) {
        descriptor = loadDescriptorFromZipOrJarFile(f, filePath, validator.ignoreMissingFile(), pathResolver);
        if (descriptor != null) {
          //is it necessary to check that only one META-INF/plugin.xml is presented?
          break;
        }
      } else if (f.isDirectory()) {
        PluginBuilder descriptor1 = loadDescriptorFromDir(f, filePath, validator.ignoreMissingFile());
        if (descriptor1 != null) {
          if (descriptor != null) {
            validator.onIncorrectStructure("Multiple META-INF/" + filePath + " found");
            return null;
          }
          descriptor = descriptor1;
        }
      }
    }

    if (descriptor == null) {
      validator.onMissingFile("Unable to find valid META-INF/" + filePath);
    }

    return descriptor;
  }

  @NotNull
  private List<URL> getInLibMetaInfUrls(File[] files) {
    List<URL> inLibJarUrls = new ArrayList<URL>();
    for (File file : files) {
      if (isJarOrZip(file)) {
        String metaInfUrl = getJarRootUrl(file) + "META-INF/";
        try {
          inLibJarUrls.add(new URL(metaInfUrl));
        } catch (MalformedURLException e) {
          LOG.warn("Unable to create URL by " + metaInfUrl, e);
        }
      }
    }
    return inLibJarUrls;
  }

  @NotNull
  @Override
  public Plugin createPlugin(@NotNull File pluginFile, boolean validatePluginXml) throws IncorrectPluginException {
    Validator validator = new PluginXmlValidator();
    if (!validatePluginXml) {
      validator = validator.ignoreMissingConfigElement();
    }

    myPluginFile = pluginFile;

    PluginBuilder builder = loadDescriptor(pluginFile, PLUGIN_XML, validator);
    if (builder != null) {
      builder.addHints(myHints);
      return builder.build();
    }
    //assert that PluginXmlValidator has thrown an appropriate exception
    throw new AssertionError("Unable to create plugin from " + pluginFile);
  }


}
