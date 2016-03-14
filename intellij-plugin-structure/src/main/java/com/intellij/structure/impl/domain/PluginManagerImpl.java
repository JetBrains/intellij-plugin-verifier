package com.intellij.structure.impl.domain;

import com.google.common.base.Predicates;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginManager;
import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.impl.resolvers.InMemoryJarResolver;
import com.intellij.structure.impl.utils.JarsUtils;
import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.impl.utils.xml.JDOMUtil;
import com.intellij.structure.impl.utils.xml.URLUtil;
import com.intellij.structure.resolvers.Resolver;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
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


  @NotNull
  private Plugin loadDescriptor(@NotNull final File file, @NotNull String fileName) throws IncorrectPluginException {
    Plugin descriptor;

    if (file.isDirectory()) {
      descriptor = loadDescriptorFromDir(file, fileName, true);
    } else if (file.exists() && isJarOrZip(file)) {
      descriptor = loadDescriptorFromZip(file, fileName, true);
    } else {
      throw new IncorrectPluginException("Incorrect plugin file type " + file + ". Should be one of .zip or .jar archives");
    }

/*

    TODO: is it necessary to load optional dependencies?
    if (descriptor != null) {
      resolveOptionalDescriptors(fileName, descriptor, new Function<String, PluginImpl>() {
        @Override
        public PluginImpl apply(String optionalDescriptorName) {
          Plugin optionalDescriptor = loadDescriptor(file, optionalDescriptorName);
          if (optionalDescriptor == null && !isJarOrZip(file)) {
            for (URL url : getClassLoaderUrls()) {
              if ("file".equals(url.getProtocol())) {
                optionalDescriptor = loadDescriptor(new File(decodeUrl(url.getFile())), optionalDescriptorName);
                if (optionalDescriptor != null) {
                  break;
                }
              }
            }
          }
          return null;
        }
      });
    }
*/

    if (descriptor == null) {
      throw new IncorrectPluginException("META-INF/" + fileName + " is not found");
    }

    return descriptor;
  }

  /*private void resolveOptionalDescriptors(@NotNull String fileName,
                                                 @NotNull PluginImpl descriptor,
                                                 @NotNull Function<String, PluginImpl> optionalDescriptorLoader) throws IncorrectPluginException {
    Map<PluginDependency, String> optionalConfigs = descriptor.getOptionalDepConfigFiles();
    if (!optionalConfigs.isEmpty()) {
      Map<String, Plugin> descriptors = new HashMap<String, Plugin>();

      for (Map.Entry<PluginDependency, String> entry : optionalConfigs.entrySet()) {
        String optName = entry.getValue();

        if (StringUtil.equal(fileName, optName)) {
          throw new IncorrectPluginException("Plugin has recursive config dependencies for descriptor " + fileName);
        }

        PluginImpl optDescriptor = optionalDescriptorLoader.apply(optName);

        if (optDescriptor != null) {
          descriptors.put(entry.getKey(), optDescriptor);
        }
        //maybe report about missing optional descriptor???
      }

      descriptor.setOptionalDescriptors(descriptors);
    }
  }*/

  @Nullable
  private Plugin checkFileInRoot(@NotNull ZipEntry entry,
                                 @NotNull String fileName,
                                 @NotNull String urlPath,
                                 @NotNull InputStream is,
                                 boolean exception) throws IncorrectPluginException {
    Matcher xmlMatcher = XML_IN_ROOT_PATTERN.matcher(entry.getName());
    if (xmlMatcher.matches()) {
      String name = xmlMatcher.group(2);
      if (StringUtil.equal(name, fileName)) {
        URL url;
        Document document;
        try {
          url = new URL(urlPath + "META-INF/" + fileName);
          document = JDOMUtil.loadDocument(URLUtil.copyInputStream(is));
        } catch (Exception e) {
          return nullOrException(exception, "Unable to read META-INF/" + fileName, e);
        }
        PluginImpl descriptor = new PluginImpl();
        descriptor.readExternal(document, url);
        return descriptor;
      }
    }
    return null;
  }

  @Nullable
  private Plugin loadFromZipStream(@NotNull ZipInputStream zipStream,
                                   @NotNull String urlPath,
                                   @NotNull String fileName,
                                   boolean exception) throws IncorrectPluginException {
    Plugin descriptorRoot = null;
    Plugin descriptorInner = null;

    try {
      ZipEntry entry;
      while ((entry = zipStream.getNextEntry()) != null) {
        if (entry.isDirectory()) {
          continue;
        }

        //firstly check xml in root (e.g. Sample.zip/Sample/META-INF/plugin.xml)
        Plugin inRoot = checkFileInRoot(entry, fileName, urlPath, zipStream, exception);
        if (inRoot != null) {
          if (descriptorRoot != null && exception) {
            throw new IncorrectPluginException("Multiple META-INF/" + fileName + " found");
          }
          descriptorRoot = inRoot;
          continue;
        }

        //secondly check .jar or .zip in lib folder (e.g. Sample/lib/Sample.jar/!...)
        if (LIB_JAR_REGEX.matcher(entry.getName()).matches()) {
          ZipInputStream inner = new ZipInputStream(zipStream);
          Plugin dinner = loadFromZipStream(inner, "jar:" + urlPath + entry.getName() + "!/", fileName, false);
          if (dinner != null) {
            descriptorInner = dinner;
          }
        }
      }

    } catch (IOException e) {
      return nullOrException(exception, "Unable to load META-INF/" + fileName, e);
    }

    if (exception && descriptorRoot != null && descriptorInner != null) {
      throw new IncorrectPluginException("Multiple META-INF/" + fileName + " found");
    }

    if (descriptorRoot != null) {
      return descriptorRoot;
    }

    if (descriptorInner != null) {
      return descriptorInner;
    }

    if (exception) {
      throw new IncorrectPluginException("META-INF/" + fileName + " is not found");
    }
    return null;
  }

  @Nullable
  private Plugin loadDescriptorFromZip(@NotNull File file, @NotNull String fileName, boolean exception) throws IncorrectPluginException {
    ZipInputStream zipInputStream = null;
    try {
      zipInputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(file)));
      String urlPath = "jar:" + StringUtil.replace(file.toURI().toASCIIString(), "!", "%21") + "!/";
      return loadFromZipStream(zipInputStream, urlPath, fileName, exception);
    } catch (IOException e) {
      throw new IncorrectPluginException("Unable to read file " + file, e);
    } finally {
      IOUtils.closeQuietly(zipInputStream);
    }
  }

  @Nullable
  private Plugin nullOrException(boolean exception, @NotNull String text) {
    return nullOrException(exception, text, null);
  }

  @Nullable
  private Plugin nullOrException(boolean exception, String text, @Nullable Exception cause) {
    if (exception) {
      throw new IncorrectPluginException(text, cause);
    }
    return null;
  }

  @Nullable
  private Plugin loadDescriptorFromDir(@NotNull final File dir, @NotNull String fileName, boolean exception) throws IncorrectPluginException {
    File descriptorFile = new File(dir, META_INF + File.separator + fileName);
    if (descriptorFile.exists()) {
      PluginImpl descriptor = new PluginImpl();
      try {
        descriptor.readExternal(descriptorFile.toURI().toURL());
      } catch (MalformedURLException e) {
        return nullOrException(exception, "File " + dir + " contains invalid plugin descriptor", e);
      }
      return descriptor;
    }
    return loadDescriptorFromLibDir(dir, fileName, exception);
  }

  @Nullable
  private Plugin loadDescriptorFromLibDir(@NotNull final File dir, @NotNull String fileName, boolean exception) {
    File libDir = new File(dir, "lib");
    if (!libDir.isDirectory()) {
      return nullOrException(exception, "Plugin `lib` directory is not found");
    }

    final File[] files = libDir.listFiles();
    if (files == null || files.length == 0) {
      return nullOrException(exception, "Plugin `lib` directory is empty");
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
        descriptor = loadDescriptorFromZip(f, fileName, false);
        if (descriptor != null) {
          break;
        }
      } else if (f.isDirectory()) {
        Plugin descriptor1 = loadDescriptorFromDir(f, fileName, false);
        if (descriptor1 != null) {
          if (descriptor != null) {
            throw new IncorrectPluginException("Two or more META-INF/plugin.xml's detected");
          }
          descriptor = descriptor1;
        }
      }
    }

    return descriptor;
  }

  @NotNull
  @Override
  public Plugin createPlugin(@NotNull File pluginFile) throws IOException, IncorrectPluginException {
    PluginImpl descriptor = (PluginImpl) loadDescriptor(pluginFile, PLUGIN_XML);
    loadClasses(pluginFile, descriptor);
    return descriptor;
  }

  private void loadClasses(@NotNull File file, @NotNull PluginImpl descriptor) {
    if (file.isDirectory()) {
      loadClassesFromDir(file, descriptor);
    } else if (file.exists() && isJarOrZip(file)) {
      loadClassesFromZip(file, descriptor);
    }
  }

  private void loadClassesFromZip(@NotNull File file, @NotNull PluginImpl descriptor) {
    ZipInputStream zipInputStream = null;
    try {
      zipInputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(file)));
      Resolver resolver = createResolverForZipStream(zipInputStream, file.getCanonicalPath());
      descriptor.setResolver(resolver);
    } catch (IOException e) {
      throw new IncorrectPluginException("Unable to read file " + file, e);
    } finally {
      IOUtils.closeQuietly(zipInputStream);
    }
  }

  @NotNull
  private Resolver createResolverForZipStream(@NotNull ZipInputStream zipStream, @NotNull String resolverName) throws IOException {
    List<Resolver> resolvers = new ArrayList<Resolver>();

    InMemoryJarResolver inMemoryJarResolver = new InMemoryJarResolver("root of " + resolverName);


    ZipEntry entry;
    while ((entry = zipStream.getNextEntry()) != null) {
      if (entry.getName().endsWith(".class")) {
        ClassNode node = getClassNodeFromInputStream(zipStream);
        inMemoryJarResolver.addClass(node);
      } else {
        Matcher matcher = LIB_JAR_REGEX.matcher(entry.getName());
        if (matcher.matches()) {
          String innerName = matcher.group(2);
          if (innerName != null) {
            ZipInputStream innerJar = new ZipInputStream(zipStream);
            resolvers.add(createResolverForZipStream(innerJar, innerName));
          }
        }
      }
    }

    if (!inMemoryJarResolver.isEmpty()) {
      resolvers.add(inMemoryJarResolver);
    }

    return Resolver.createUnionResolver(resolverName, resolvers);
  }

  private void loadClassesFromDir(@NotNull File dir, @NotNull PluginImpl descriptor) throws IncorrectPluginException {
    File classesDir = new File(dir, "classes");

    List<Resolver> resolvers = new ArrayList<Resolver>();

    if (classesDir.exists()) {
      Collection<File> classFiles = FileUtils.listFiles(classesDir, new String[]{"class"}, true);
      InMemoryJarResolver rootResolver = new InMemoryJarResolver("Plugin root classes of " + descriptor.getPluginId());
      for (File file : classFiles) {
        InputStream is = null;
        try {
          is = FileUtils.openInputStream(file);
          ClassNode node = getClassNodeFromInputStream(is);
          rootResolver.addClass(node);
        } catch (IOException e) {
          throw new IncorrectPluginException("Unable to read class file " + file, e);
        } finally {
          IOUtils.closeQuietly(is);
        }
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
      throw new IncorrectPluginException("Unable to read `lib` directory", e);
    }

    descriptor.setResolver(Resolver.createUnionResolver("Plugin resolver " + descriptor.getPluginId(), resolvers));
  }

  @NotNull
  private ClassNode getClassNodeFromInputStream(@NotNull InputStream is) throws IOException {
    ClassNode node = new ClassNode();
    new ClassReader(is).accept(node, 0);
    return node;
  }

}
