package com.intellij.structure.impl.domain;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginDependency;
import com.intellij.structure.domain.PluginManager;
import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.impl.resolvers.FilesResolver;
import com.intellij.structure.impl.resolvers.ZipResolver;
import com.intellij.structure.impl.utils.JarsUtils;
import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.impl.utils.validators.OptionalXmlValidator;
import com.intellij.structure.impl.utils.validators.PluginXmlValidator;
import com.intellij.structure.impl.utils.validators.Validator;
import com.intellij.structure.resolvers.Resolver;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
  private static final String META_INF = "META-INF";
  private static final Pattern XML_IN_ROOT_PATTERN = Pattern.compile("([^/]*/)?META-INF/((\\w|\\-)+\\.xml)");

  private static boolean isJarOrZip(@NotNull File file) {
    if (file.isDirectory()) {
      return false;
    }
    final String name = file.getName();
    return StringUtil.endsWithIgnoreCase(name, ".jar") || StringUtil.endsWithIgnoreCase(name, ".zip");
  }


  @Nullable
  private Plugin loadDescriptor(@NotNull final File file, @NotNull String fileName, @NotNull Validator validator) throws IncorrectPluginException {
    Plugin descriptor;

    if (file.isDirectory()) {
      descriptor = loadDescriptorFromDir(file, fileName, validator);
    } else if (file.exists() && isJarOrZip(file)) {
      descriptor = loadDescriptorFromZipFile(file, fileName, validator);
    } else {
      validator.onIncorrectStructure("Incorrect plugin file type " + file + ". Should be a .zip or .jar archive or a directory.");
      return null;
    }

    if (descriptor != null) {
      resolveOptionalDescriptors(fileName, (PluginImpl) descriptor, validator, new Function<String, PluginImpl>() {
        @Override
        @Nullable
        public PluginImpl apply(@Nullable String optionalDescriptorName) throws IncorrectPluginException {
          if (optionalDescriptorName != null) {
            return (PluginImpl) loadDescriptor(file, optionalDescriptorName, new OptionalXmlValidator());
          }
          return null;
        }
      });
    }

    if (descriptor == null) {
      validator.onMissingFile("META-INF/" + fileName + " is not found");
    }

    return descriptor;
  }

  private void resolveOptionalDescriptors(@NotNull String fileName,
                                          @NotNull PluginImpl descriptor,
                                          @NotNull Validator validator,
                                          @NotNull Function<String, PluginImpl> optionalDescriptorLoader) throws IncorrectPluginException {

    Map<PluginDependency, String> optionalConfigs = descriptor.getOptionalDependenciesConfigFiles();
    if (!optionalConfigs.isEmpty()) {
      Map<String, PluginImpl> descriptors = new HashMap<String, PluginImpl>();

      for (Map.Entry<PluginDependency, String> entry : optionalConfigs.entrySet()) {
        String optFileName = entry.getValue();

        if (StringUtil.equal(fileName, optFileName)) {
          validator.onIncorrectStructure("Plugin has recursive config dependencies for descriptor " + fileName);
        }

        PluginImpl optDescriptor;
        try {
          optDescriptor = optionalDescriptorLoader.apply(optFileName);
        } catch (IncorrectPluginException e) {
          //maybe log somehow?
          continue;
        }

        if (optDescriptor != null) {
          descriptors.put(optFileName, optDescriptor);
        }
      }

      descriptor.setOptionalDescriptors(descriptors);
    }
  }

  @Nullable
  private Plugin checkFileInRoot(@NotNull ZipEntry entry,
                                 @NotNull String fileName,
                                 @NotNull String urlPath,
                                 @NotNull Validator validator) throws IncorrectPluginException {
    Matcher xmlMatcher = XML_IN_ROOT_PATTERN.matcher(entry.getName());
    if (xmlMatcher.matches()) {
      String name = xmlMatcher.group(2);
      if (StringUtil.equal(name, fileName)) {
        PluginImpl descriptor = new PluginImpl();
        try {
          URL url = new URL(urlPath + entry.getName());
          descriptor.readExternal(url, validator);
        } catch (Exception e) {
          validator.onCheckedException("Unable to read META-INF/" + fileName, e);
          return null;
        }
        return descriptor;
      }
    }
    return null;
  }

  @Nullable
  private Plugin loadFromZipStream(@NotNull ZipInputStream zipStream,
                                   @NotNull String zipRootUrl,
                                   @NotNull String fileName,
                                   @NotNull Validator validator) throws IncorrectPluginException {
    Plugin descriptor = null;

    try {
      ZipEntry entry;
      while ((entry = zipStream.getNextEntry()) != null) {
        if (entry.isDirectory()) {
          continue;
        }

        Plugin inRoot = checkFileInRoot(entry, fileName, zipRootUrl, validator);
        if (inRoot != null) {
          if (descriptor != null) {
            validator.onIncorrectStructure("Multiple META-INF/" + fileName + " found");
            return null;
          }
          descriptor = inRoot;
        }
      }
    } catch (IOException e) {
      validator.onCheckedException("Unable to load META-INF/" + fileName, e);
      return null;
    }

    if (descriptor == null) {
      validator.onMissingFile("META-INF/" + fileName + " is not found");
    }

    return descriptor;
  }

  @Nullable
  private Plugin loadDescriptorFromZipFile(@NotNull File file, @NotNull String fileName, @NotNull Validator validator) throws IncorrectPluginException {
    final String zipRootUrl = "jar:" + StringUtil.replace(file.toURI().toASCIIString(), "!", "%21") + "!/";

    Plugin descriptorRoot = null;
    Plugin descriptorInner = null;

    ZipFile zipFile = null;
    try {
      zipFile = new ZipFile(file);
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (entry.isDirectory()) {
          continue;
        }

        Plugin inRoot = checkFileInRoot(entry, fileName, zipRootUrl, validator.getMissingFileIgnoringValidator());
        if (inRoot != null) {
          if (descriptorRoot != null) {
            validator.onIncorrectStructure("Multiple META-INF/" + fileName + " found");
            return null;
          }
          descriptorRoot = inRoot;
          continue;
        }

        if (LIB_JAR_REGEX.matcher(entry.getName()).matches()) {
          ZipInputStream inner = new ZipInputStream(zipFile.getInputStream(entry));
          Plugin dinner = loadFromZipStream(inner, "jar:" + zipRootUrl + entry.getName() + "!/", fileName, validator.getMissingFileIgnoringValidator());
          if (dinner != null) {
            descriptorInner = dinner;
          }
        }
      }
    } catch (IOException e) {
      validator.onCheckedException("Unable to read file " + file, e);
      return null;
    } finally {
      IOUtils.closeQuietly(zipFile);
    }

    if (descriptorRoot != null && descriptorInner != null) {
      validator.onIncorrectStructure("Multiple META-INF/" + fileName + " found");
      return null;
    }

    if (descriptorRoot != null) {
      return descriptorRoot;
    }

    if (descriptorInner != null) {
      return descriptorInner;
    }

    validator.onMissingFile("META-INF/" + fileName + " is not found");
    return null;
  }

  @Nullable
  private Plugin loadDescriptorFromDir(@NotNull final File dir, @NotNull String fileName, @NotNull Validator validator) throws IncorrectPluginException {
    File descriptorFile = new File(dir, META_INF + File.separator + fileName);
    if (descriptorFile.exists()) {
      PluginImpl descriptor = new PluginImpl();
      try {
        descriptor.readExternal(descriptorFile.toURI().toURL(), validator);
      } catch (MalformedURLException e) {
        validator.onCheckedException("File " + dir + " contains invalid plugin descriptor " + fileName, e);
        return null;
      }
      return descriptor;
    }
    return loadDescriptorFromLibDir(dir, fileName, validator);
  }

  @Nullable
  private Plugin loadDescriptorFromLibDir(@NotNull final File dir, @NotNull String fileName, @NotNull Validator validator) throws IncorrectPluginException {
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
        descriptor = loadDescriptorFromZipFile(f, fileName, validator.getMissingFileIgnoringValidator());
        if (descriptor != null) {
          break;
        }
      } else if (f.isDirectory()) {
        Plugin descriptor1 = loadDescriptorFromDir(f, fileName, validator.getMissingFileIgnoringValidator());
        if (descriptor1 != null) {
          if (descriptor != null) {
            validator.onIncorrectStructure("Two or more META-INF/" + fileName + " detected");
            return null;
          }
          descriptor = descriptor1;
        }
        }
    }

    if (descriptor == null) {
      validator.onMissingFile("Unable to find valid META-INF/" + fileName);
    }

    return descriptor;
  }

  @NotNull
  @Override
  public Plugin createPlugin(@NotNull File pluginFile) throws IncorrectPluginException {
    return createPlugin(pluginFile, true);
  }

  @NotNull
  private Plugin createPlugin(@NotNull File pluginFile, boolean loadClasses) throws IncorrectPluginException {
    Validator validator = new PluginXmlValidator();
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

  @NotNull
  @Override
  public Plugin createPluginWithEmptyResolver(@NotNull File pluginFile) throws IOException, IncorrectPluginException {
    return createPlugin(pluginFile, false);
  }

  private void loadClasses(@NotNull File file, @NotNull PluginImpl descriptor, @NotNull Validator validator) {
    if (file.isDirectory()) {
      loadClassesFromDir(file, descriptor, validator);
    } else if (file.exists() && isJarOrZip(file)) {
      loadClassesFromZip(file, descriptor, validator);
    }
  }

  private void loadClassesFromZip(@NotNull File file, @NotNull PluginImpl descriptor, @NotNull Validator validator) {
    ZipInputStream zipInputStream = null;
    try {
      zipInputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(file)));
      String urlPath = StringUtil.replace(file.toURI().toASCIIString(), "!", "%21");
      Resolver resolver = createResolverForZipStream(zipInputStream, urlPath, file.getCanonicalPath());
      descriptor.setResolver(resolver);
    } catch (IOException e) {
      validator.onCheckedException("Unable to read file " + file, e);
    } finally {
      IOUtils.closeQuietly(zipInputStream);
    }
  }

  @NotNull
  private Resolver createResolverForZipStream(@NotNull ZipInputStream zipStream,
                                              @NotNull String urlPath,
                                              @NotNull String resolverName) throws IOException {
    List<Resolver> resolvers = new ArrayList<Resolver>();

    Resolver rootResolver = new ZipResolver(resolverName, urlPath);

    ZipEntry entry;
    while ((entry = zipStream.getNextEntry()) != null) {
        Matcher matcher = LIB_JAR_REGEX.matcher(entry.getName());
        if (matcher.matches()) {
          String innerName = matcher.group(2);
          if (innerName != null) {
            ZipInputStream innerJar = new ZipInputStream(zipStream);
            String innerJarUrl = "jar:" + urlPath + "!/" + StringUtil.trimStart(entry.getName(), "/");
            resolvers.add(createResolverForZipStream(innerJar, innerJarUrl, innerName));
          }
        }
    }

    if (!rootResolver.isEmpty()) {
      resolvers.add(rootResolver);
    }

    return Resolver.createUnionResolver(resolverName, resolvers);
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
        List<ZipFile> jars = JarsUtils.collectJarsRecursively(lib, Predicates.<File>alwaysTrue());
        Resolver libResolver = JarsUtils.makeResolver("Plugin `lib` jars: " + lib.getCanonicalPath(), jars);
        resolvers.add(libResolver);
      }
    } catch (IOException e) {
      validator.onCheckedException("Unable to read `lib` directory", e);
    }

    descriptor.setResolver(Resolver.createUnionResolver("Plugin resolver " + descriptor.getPluginId(), resolvers));
  }


}
