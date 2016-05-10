package com.intellij.structure.impl.domain;

import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginDependency;
import com.intellij.structure.domain.PluginManager;
import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.impl.resolvers.FilesResolver;
import com.intellij.structure.impl.resolvers.ZipResolver;
import com.intellij.structure.impl.utils.JarsUtils;
import com.intellij.structure.impl.utils.Pair;
import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.impl.utils.validators.PluginXmlValidator;
import com.intellij.structure.impl.utils.validators.Validator;
import com.intellij.structure.impl.utils.xml.JDOMUtil;
import com.intellij.structure.resolvers.Resolver;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;


/**
 * @author Sergey Patrikeev
 */
public class PluginManagerImpl extends PluginManager {

  private static final String PLUGIN_XML = "plugin.xml";
  private static final Pattern LIB_JAR_REGEX = Pattern.compile("([^/]+/)?lib/([^/]+\\.(jar|zip))");
  private static final Pattern CLASSES_DIR_REGEX = Pattern.compile("([^/]+/)?classes/");

  private static final Pattern XML_IN_META_INF_PATTERN = Pattern.compile("([^/]*/)?META-INF/(([^/]+/)*(\\w|\\-)+\\.xml)");

  /**
   * <p>Contains all the .xml files under META-INF/ directory and subdirectories.</p> It consists of the following
   * entries: (file path relative to META-INF/ dir) TO pair of (full URL path of the file) and (corresponding Document)
   * <p>It will be used later to resolve optional descriptors.</p>
   */
  private final Map<String, Pair<String, Document>> myRootXmlDocuments = new HashMap<String, Pair<String, Document>>();

  private static boolean isJarOrZip(@NotNull File file) {
    if (file.isDirectory()) {
      return false;
    }
    final String name = file.getName();
    return StringUtil.endsWithIgnoreCase(name, ".jar") || StringUtil.endsWithIgnoreCase(name, ".zip");
  }

  @NotNull
  private static String getFileEscapedUri(@NotNull File file) {
    return StringUtil.replace(file.toURI().toASCIIString(), "!", "%21");
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
  private Plugin loadDescriptor(@NotNull final File file, @NotNull String filePath, @NotNull Validator validator) throws IncorrectPluginException {
    filePath = StringUtil.toSystemIndependentName(filePath);

    Plugin descriptor;

    if (file.isDirectory()) {
      descriptor = loadDescriptorFromDir(file, filePath, validator);
    } else if (file.exists() && isJarOrZip(file)) {
      descriptor = loadDescriptorFromZipFile(file, filePath, validator);
    } else {
      if (!file.exists()) {
        validator.onIncorrectStructure("Plugin file is not found " + file);
      } else {
        validator.onIncorrectStructure("Incorrect plugin file type " + file + ". Should be a .zip or .jar archive or a directory.");
      }
      return null;
    }

    if (descriptor != null) {
      resolveOptionalDescriptors(file, filePath, (PluginImpl) descriptor, validator);
    }

    if (descriptor == null) {
      validator.onMissingFile("META-INF/" + filePath + " is not found");
    }

    return descriptor;
  }

  private void resolveOptionalDescriptors(@NotNull File file,
                                          @NotNull String filePath,
                                          @NotNull PluginImpl descriptor,
                                          @NotNull Validator parentValidator) throws IncorrectPluginException {
    Map<PluginDependency, String> optionalConfigs = descriptor.getOptionalDependenciesConfigFiles();
    if (!optionalConfigs.isEmpty()) {
      Map<String, PluginImpl> descriptors = new HashMap<String, PluginImpl>();

      for (Map.Entry<PluginDependency, String> entry : optionalConfigs.entrySet()) {
        String optFilePath = entry.getValue();

        if (StringUtil.equal(filePath, optFilePath)) {
          parentValidator.onIncorrectStructure("Plugin has recursive config dependencies for descriptor " + filePath);
        }

        Pair<String, Document> xmlPair = myRootXmlDocuments.get(optFilePath);
        if (xmlPair != null) {
          try {
            URL url = new URL(xmlPair.getFirst());
            Document document = xmlPair.getSecond();
            PluginImpl optDescriptor = new PluginImpl();
            optDescriptor.readExternal(document, url, parentValidator.ignoreMissingConfigElement());
            descriptors.put(optFilePath, optDescriptor);
          } catch (MalformedURLException e) {
            parentValidator.onCheckedException("Unable to read META-INF/" + optFilePath, e);
          }
        } else {
          //don't complain if the file is not found and don't complain if it has incorrect .xml structure
          Validator optValidator = parentValidator.ignoreMissingConfigElement().ignoreMissingFile();

          PluginImpl optDescriptor = (PluginImpl) loadDescriptor(file, optFilePath, optValidator);

          //in IDEA there is a one more attempt to load descriptor (loadDescriptorFromResource) but I don't know why

          if (optDescriptor != null) {
            descriptors.put(optFilePath, optDescriptor);
          } else {
            System.err.println("Optional descriptor META-INF/" + optFilePath + " is not found");
          }
        }
      }

      descriptor.setOptionalDescriptors(descriptors);
    }
  }

  //filePath is relative to META-INF/ => should resolve it properly

  /**
   * Checks than the given {@code entry} corresponds to the sought-for file specified with {@code filePath}.
   *
   * @param entry               current entry in the overlying traversing of zip file
   * @param filePath            sought-for file, path is relative to META-INF/ directory
   * @param rootUrl             url corresponding to the root of the zip file from which this {@code entry} come
   * @param validator           problems resolver
   * @param entryStreamSupplier supplies the input stream for this entry if needed
   * @return sought-for descriptor or null
   * @throws IncorrectPluginException if incorrect plugin structure
   */
  @Nullable
  private Plugin loadDescriptorFromEntry(@NotNull ZipEntry entry,
                                         @NotNull String filePath,
                                         @NotNull String rootUrl,
                                         @NotNull Validator validator,
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
        validator.onCheckedException("Unable to read META-INF/" + name, e);
        return null;
      }

      if (StringUtil.equal(name, filePath)) {
        PluginImpl descriptor = new PluginImpl();
        descriptor.readExternal(document, url, validator);
        return descriptor;
      } else {
        //add this .xml for the future check
        myRootXmlDocuments.put(name, Pair.create(xmlUrl, document));
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

          PluginImpl descriptor = new PluginImpl();
          descriptor.readExternal(document, url, validator);
          return descriptor;
        } catch (RuntimeException e) {
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
  private Plugin loadFromZipStream(@NotNull final ZipInputStream zipStream,
                                   @NotNull String zipRootUrl,
                                   @NotNull String filePath,
                                   @NotNull Validator validator) throws IncorrectPluginException {
    Plugin descriptor = null;

    try {
      ZipEntry entry;
      while ((entry = zipStream.getNextEntry()) != null) {
        if (entry.isDirectory()) {
          continue;
        }

        Plugin inRoot = loadDescriptorFromEntry(entry, filePath, zipRootUrl, validator, new Supplier<InputStream>() {
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

  @Nullable
  private Plugin loadDescriptorFromZipFile(@NotNull final File file, @NotNull final String filePath, @NotNull final Validator validator) throws IncorrectPluginException {
    final String zipRootUrl = "jar:" + getFileEscapedUri(file) + "!/";

    Plugin descriptorRoot = null;
    Plugin descriptorInner = null;

    ZipFile zipFile = null;
    try {
      zipFile = new ZipFile(file);
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        final ZipEntry entry = entries.nextElement();
        if (entry.isDirectory()) {
          continue;
        }

        final ZipFile finalZipFile = zipFile;
        Plugin inRoot = loadDescriptorFromEntry(entry, filePath, zipRootUrl, validator.ignoreMissingFile(), new Supplier<InputStream>() {
          @Override
          public InputStream get() {
            try {
              return finalZipFile.getInputStream(entry);
            } catch (IOException e) {
              validator.onCheckedException("Unable to read META-INF/" + filePath, e);
              return null;
            }
          }
        });

        if (inRoot != null) {
          if (descriptorRoot != null) {
            validator.onIncorrectStructure("Multiple META-INF/" + filePath + " found");
            return null;
          }
          descriptorRoot = inRoot;
          continue;
        }

        if (LIB_JAR_REGEX.matcher(entry.getName()).matches()) {
          ZipInputStream inner = new ZipInputStream(zipFile.getInputStream(entry));
          Plugin innerDescriptor = loadFromZipStream(inner, "jar:" + zipRootUrl + entry.getName() + "!/", filePath, validator.ignoreMissingFile());
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
        if (zipFile != null) {
          zipFile.close();
        }
      } catch (IOException ignored) {
      }
    }

    if (descriptorRoot != null && descriptorInner != null) {
      validator.onIncorrectStructure("Multiple META-INF/" + filePath + " found");
      return null;
    }

    if (descriptorRoot != null) {
      return descriptorRoot;
    }

    if (descriptorInner != null) {
      return descriptorInner;
    }

    validator.onMissingFile("META-INF/" + filePath + " is not found");
    return null;
  }

  @Nullable
  private Plugin loadDescriptorFromDir(@NotNull final File dir, @NotNull String filePath, @NotNull Validator validator) throws IncorrectPluginException {
    File descriptorFile = new File(dir, "META-INF" + File.separator + StringUtil.toSystemDependentName(filePath));
    if (descriptorFile.exists()) {

      Collection<File> allXmlUnderMetaInf = FileUtils.listFiles(descriptorFile.getParentFile(), new String[]{"xml"}, true);
      for (File xml : allXmlUnderMetaInf) {
        InputStream inputStream = null;
        try {
          inputStream = FileUtils.openInputStream(xml);
          Document document = JDOMUtil.loadDocument(inputStream);
          String uri = getFileEscapedUri(xml);
          myRootXmlDocuments.put(xml.getName(), Pair.create(uri, document));
        } catch (Exception e) {
          if (StringUtil.equal(xml.getName(), StringUtil.getFileName(filePath))) {
            validator.onCheckedException("Unable to read .xml file META-INF/" + filePath, e);
          }
        } finally {
          IOUtils.closeQuietly(inputStream);
        }
      }

      PluginImpl descriptor = new PluginImpl();
      try {
        descriptor.readExternal(descriptorFile.toURI().toURL(), validator);
      } catch (MalformedURLException e) {
        validator.onCheckedException("File " + dir + " contains invalid plugin descriptor " + filePath, e);
        return null;
      }
      return descriptor;
    }
    return loadDescriptorFromLibDir(dir, filePath, validator);
  }

  @Nullable
  private Plugin loadDescriptorFromLibDir(@NotNull final File dir, @NotNull String filePath, @NotNull Validator validator) throws IncorrectPluginException {
    File libDir = new File(dir, "lib");
    if (!libDir.isDirectory()) {
      validator.onMissingFile("Plugin `lib` directory is not found");
      return null;
    }

    final File[] files = libDir.listFiles();
    if (files == null || files.length == 0) {
      validator.onIncorrectStructure("Plugin `lib` directory is empty");
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

    Plugin descriptor = null;

    for (final File f : files) {
      if (isJarOrZip(f)) {
        descriptor = loadDescriptorFromZipFile(f, filePath, validator.ignoreMissingFile());
        if (descriptor != null) {
          break;
        }
      } else if (f.isDirectory()) {
        Plugin descriptor1 = loadDescriptorFromDir(f, filePath, validator.ignoreMissingFile());
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
  @Override
  public Plugin createPlugin(@NotNull File pluginFile, boolean validatePluginXml, boolean loadClasses) throws IncorrectPluginException {
    Validator validator = new PluginXmlValidator();
    if (!validatePluginXml) {
      validator = validator.ignoreMissingConfigElement();
    }

    PluginImpl descriptor = (PluginImpl) loadDescriptor(pluginFile, PLUGIN_XML, validator);
    if (descriptor != null) {
      if (loadClasses) {
        loadClasses(pluginFile, descriptor, validator);
      }
      return descriptor;
    }
    //assert that PluginXmlValidator has thrown an appropriate exception
    throw new AssertionError("Unable to create plugin from " + pluginFile);
  }

  private void loadClasses(@NotNull File file, @NotNull PluginImpl descriptor, @NotNull Validator validator) {
    if (file.isDirectory()) {
      loadClassesFromDir(file, descriptor, validator);
    } else if (file.exists() && isJarOrZip(file)) {
      loadClassesFromZip(file, descriptor, validator);
    }
  }

  private void loadClassesFromZip(@NotNull File file, @NotNull PluginImpl descriptor, @NotNull Validator validator) {
    String zipUrl = getFileEscapedUri(file);

    List<Resolver> resolvers = new ArrayList<Resolver>();

    Resolver classesDirResolver = null;

    ZipFile zipFile = null;
    try {
      zipFile = new ZipFile(file);
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();

        //check if classes directory
        Matcher classesDirMatcher = CLASSES_DIR_REGEX.matcher(entry.getName());
        if (classesDirMatcher.matches()) {
          String rootDir = entry.getName();
          try {
            classesDirResolver = new ZipResolver("Plugin classes directory", zipUrl, rootDir);
          } catch (IOException e) {
            validator.onCheckedException("Unable to read plugin classes from " + rootDir, e);
            return;
          }
          if (!classesDirResolver.isEmpty()) {
            resolvers.add(classesDirResolver);
          }
        }

        //check if jar in lib/ directory
        Matcher libDirMatcher = LIB_JAR_REGEX.matcher(entry.getName());
        if (libDirMatcher.matches()) {
          String innerName = libDirMatcher.group(2);
          if (innerName != null) {
            String innerJarUrl = "jar:" + zipUrl + "!/" + StringUtil.trimStart(entry.getName(), "/");
            ZipResolver innerResolver;
            try {
              innerResolver = new ZipResolver(innerName, innerJarUrl, ".");
            } catch (IOException e) {
              validator.onCheckedException("Unable to read plugin classes from " + entry.getName(), e);
              return;
            }
            if (!innerResolver.isEmpty()) {
              resolvers.add(innerResolver);
            }
          }
        }
      }
    } catch (IOException e) {
      validator.onCheckedException("Unable to read plugin classes from " + file.getName(), e);
      return;
    } finally {
      if (zipFile != null) {
        try {
          zipFile.close();
        } catch (IOException ignored) {
        }
      }
    }

    //check if this zip archive is actually a .jar archive (someone has changed its extension from .jar to .zip)
    if (classesDirResolver == null) {
      try {
        ZipResolver rootResolver = new ZipResolver(file.getName(), zipUrl, ".");
        if (!rootResolver.isEmpty()) {
          resolvers.add(rootResolver);
        }
      } catch (IOException e) {
        validator.onCheckedException("Unable to read plugin classes from " + file.getName(), e);
      }
    }


    descriptor.setResolver(Resolver.createUnionResolver("Plugin resolver " + descriptor.getPluginId(), resolvers));
  }

  private void loadClassesFromDir(@NotNull File dir, @NotNull PluginImpl descriptor, @NotNull Validator validator) throws IncorrectPluginException {
    File classesDir = new File(dir, "classes");

    List<Resolver> resolvers = new ArrayList<Resolver>();

    if (classesDir.exists()) {
      Collection<File> classFiles = FileUtils.listFiles(classesDir, new String[]{"class"}, true);
      Resolver rootResolver;
      try {
        rootResolver = new FilesResolver("Plugin `classes` directory of " + descriptor.getPluginId(), classFiles);
      } catch (IOException e) {
        validator.onCheckedException("Unable to read `classes` directory classes", e);
        return;
      }

      if (!rootResolver.isEmpty()) {
        resolvers.add(rootResolver);
      }
    }

    try {
      File lib = new File(dir, "lib");
      if (lib.isDirectory()) {
        Collection<File> jars = JarsUtils.collectJars(lib, Predicates.<File>alwaysTrue(), true);
        Resolver libResolver = JarsUtils.makeResolver("Plugin `lib` jars: " + lib.getCanonicalPath(), jars);
        resolvers.add(libResolver);
      }
    } catch (IOException e) {
      validator.onCheckedException("Unable to read `lib` directory", e);
      return;
    }

    descriptor.setResolver(Resolver.createUnionResolver("Plugin resolver " + descriptor.getPluginId(), resolvers));
  }


}
