package com.intellij.structure.impl.domain;

import com.google.common.base.Predicates;
import com.google.common.io.ByteStreams;
import com.intellij.structure.bytecode.ClassFile;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginManager;
import com.intellij.structure.errors.BrokenPluginException;
import com.intellij.structure.impl.pool.ContainerClassPool;
import com.intellij.structure.impl.pool.InMemoryJarClassPool;
import com.intellij.structure.impl.pool.JarClassPool;
import com.intellij.structure.pool.ClassPool;
import com.intellij.structure.utils.StringUtil;
import com.intellij.structure.utils.Util;
import com.intellij.structure.utils.xml.JDOMUtil;
import com.intellij.structure.utils.xml.JDOMXIncluder;
import com.intellij.structure.utils.xml.XIncludeException;
import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Sergey Patrikeev
 */
public class IdeaPluginManager extends PluginManager {
  private static final String META_INF_ENTRY = "META-INF/";
  private static final String PLUGIN_XML_ENTRY_NAME = META_INF_ENTRY + "plugin.xml";
  private static final String CLASS_SUFFIX = ".class";
  private static final Pattern XML_IN_ROOT_PATTERN = Pattern.compile("([^/]*/)?META-INF/.+\\.xml");

  @NotNull
  private static IdeaPlugin createFromZip(@NotNull File zipFile) throws IOException, BrokenPluginException {
    byte[] pluginXmlBytes = null;
    ClassPool pluginClassPool = null;
    List<ClassPool> libraryPool = new ArrayList<ClassPool>();
    URL pluginXmlUrl = null;
    Map<String, Document> allXmlInRoot = new HashMap<String, Document>();

    InMemoryJarClassPool zipRootPool = new InMemoryJarClassPool("PLUGIN_ROOT: " + zipFile.getAbsolutePath());

    final ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));

    try {
      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        if (entry.isDirectory()) continue;

        String entryName = entry.getName();

        if (entryName.endsWith(CLASS_SUFFIX)) {
          //simply .class-file
          ClassFile classFile = new ClassFile(zipInputStream);
          zipRootPool.addClass(classFile);

        } else if (isXmlInRoot(entryName)) {
          //some *.xml file (maybe META-INF/plugin.xml)

          final byte[] data = ByteStreams.toByteArray(zipInputStream);
          final URL xmlUrl = new URL(
              "jar:" + StringUtil.replace(zipFile.toURI().toASCIIString(), "!", "%21") + "!/" + entryName
          );

          boolean isPluginXml = entryName.endsWith(PLUGIN_XML_ENTRY_NAME);

          if (isPluginXml) {
            //this is the main plugin.xml i.e. META-INF/plugin.xml

            if (pluginXmlBytes != null && !Arrays.equals(data, pluginXmlBytes)) {
              throw new BrokenPluginException("Plugin has more than one jars with plugin.xml");
            }
            pluginXmlBytes = data;
            pluginClassPool = zipRootPool;
            pluginXmlUrl = xmlUrl;
          }

          //if it is the first .xml-file
          tryAddXmlInRoot(allXmlInRoot, entryName, data, xmlUrl, isPluginXml);

        } else if (entryName.matches("([^/]+/)?lib/[^/]+\\.jar")) {
          ZipInputStream innerJar = new ZipInputStream(zipInputStream);

          final InMemoryJarClassPool innerPool = new InMemoryJarClassPool(entryName);
          final Map<String, Document> innerDocuments = new HashMap<String, Document>();

          ZipEntry innerEntry;
          while ((innerEntry = innerJar.getNextEntry()) != null) {
            String name = innerEntry.getName();

            if (name.startsWith(META_INF_ENTRY) && name.endsWith(".xml")) {
              //some xml

              final byte[] data = ByteStreams.toByteArray(innerJar);
              final URL xmlUrl = new URL("jar:" + StringUtil.replace(zipFile.toURI().toASCIIString(), "!", "%21") + "!/" + entryName + "!/META-INF/plugin.xml");

              boolean isPluginXml = name.endsWith(PLUGIN_XML_ENTRY_NAME);
              if (isPluginXml) {
                if (pluginXmlBytes != null && !Arrays.equals(data, pluginXmlBytes)) {
                  throw new BrokenPluginException("Plugin has more than one jars with plugin.xml");
                }

                pluginXmlBytes = data; //the main plugin.xml
                pluginClassPool = innerPool; //pluginPool is in this .jar exactly
                allXmlInRoot = innerDocuments; //and documents are based on this META-INF/* directory

                pluginXmlUrl = xmlUrl;
              }

              //parse a document and add to list of all xml-s
              tryAddXmlInRoot(innerDocuments, name, data, xmlUrl, isPluginXml);

            } else if (name.endsWith(CLASS_SUFFIX)) {
              innerPool.addClass(name.substring(0, name.length() - CLASS_SUFFIX.length()), ByteStreams.toByteArray(innerJar));
            }
          }

          if (innerPool != pluginClassPool) {
            libraryPool.add(innerPool);
          }
        }
      }
    } finally {
      IOUtils.closeQuietly(zipInputStream);
    }

    if (pluginXmlBytes == null) {
      throw new BrokenPluginException("No plugin.xml found for plugin " + zipFile.getPath());
    }

    Document pluginXml;

    try {
      pluginXml = JDOMUtil.loadDocument(new ByteArrayInputStream(pluginXmlBytes));
      pluginXml = JDOMXIncluder.resolve(pluginXml, pluginXmlUrl.toExternalForm());
    } catch (JDOMException e) {
      throw new BrokenPluginException("Invalid plugin.xml", e);
    } catch (XIncludeException e) {
      throw new BrokenPluginException("Failed to read plugin.xml", e);
    }

    if (!zipRootPool.isEmpty()) {
      if (pluginClassPool != zipRootPool) {
        throw new BrokenPluginException("Plugin contains .class files in the root, but has no META-INF/plugin.xml");
      }
    }

    return new IdeaPlugin(zipFile, pluginClassPool, ContainerClassPool.getUnion(zipFile.getPath(), libraryPool), pluginXml, allXmlInRoot);
  }

  private static void tryAddXmlInRoot(@NotNull Map<String, Document> container,
                                      @NotNull String entryName,
                                      @NotNull byte[] data,
                                      @NotNull URL xmlUrl,
                                      boolean isPluginXml) throws BrokenPluginException {
    try {
      Document document = JDOMUtil.loadDocument(new ByteArrayInputStream(data));
      document = JDOMXIncluder.resolve(document, xmlUrl.toExternalForm());
      container.put(entryName, document);
    } catch (Exception e) {
      if (isPluginXml) {
        //plugin.xml should be correct!
        throw new BrokenPluginException("Failed to read and parse META-INF/plugin.xml", e);
      }
      //for non-main .xml it's not critical
      System.out.println("Couldn't parse " + entryName);
    }
  }

  private static boolean isXmlInRoot(@NotNull String entryName) {
    return XML_IN_ROOT_PATTERN.matcher(entryName).matches();
  }

  @NotNull
  private static Plugin createFromJar(@NotNull File jarFile) throws IOException, BrokenPluginException {
    return createFromJars(jarFile, Collections.singletonList(new JarFile(jarFile)));
  }

  private static Plugin createFromJars(@NotNull File pluginFile,
                                       @NotNull List<JarFile> jarFiles) throws BrokenPluginException, IOException {
    Document pluginXml = null;
    ClassPool pluginClassPool = null;
    List<ClassPool> libraryPools = new ArrayList<ClassPool>();

    try {
      for (JarFile jar : jarFiles) {
        ZipEntry pluginXmlEntry = jar.getEntry(PLUGIN_XML_ENTRY_NAME);
        if (pluginXmlEntry != null) {
          if (pluginClassPool != null) {
            throw new BrokenPluginException("Plugin has more than one .jar with plugin.xml: " + pluginFile);
          }

          pluginClassPool = new JarClassPool(jar);
          URL jarURL = new URL(
              "jar:" + StringUtil.replace(new File(jar.getName()).toURI().toASCIIString(), "!", "%21") + "!/META-INF/plugin.xml"
          );

          try {
            pluginXml = JDOMUtil.loadDocument(jarURL);
            pluginXml = JDOMXIncluder.resolve(pluginXml, jarURL.toExternalForm());
          } catch (JDOMException e) {
            throw new BrokenPluginException("Invalid plugin.xml", e);
          } catch (XIncludeException e) {
            throw new BrokenPluginException("Failed to read plugin.xml", e);
          }
        } else {
          libraryPools.add(new JarClassPool(jar));
        }
      }
    } catch (Exception exc) {
      throw new BrokenPluginException(exc);
    }

    if (pluginXml == null) {
      throw new BrokenPluginException("No plugin.xml found for plugin: " + pluginFile);
    }

    final Map<String, Document> xmlDocumentsInRoot = new HashMap<String, Document>();
    //this is a correct plugin (and only one META-INF/plugin.xml) found for it
    for (JarFile jarFile : jarFiles) {
      ZipEntry pluginXmlEntry = jarFile.getEntry(PLUGIN_XML_ENTRY_NAME);
      if (pluginXmlEntry != null) {
        //this is the very JAR for which META-INF/plugin.xml found so let's take the other .xml-s

        for (JarEntry jarEntry : Collections.list(jarFile.entries())) {
          String name = jarEntry.getName();
          if (name.startsWith(META_INF_ENTRY) && name.endsWith(".xml")) {
            try {
              URL url = new URL(
                  "jar:" + StringUtil.replace(new File(jarFile.getName()).toURI().toASCIIString(), "!", "%21") + "!/" + name
              );

              Document document = JDOMUtil.loadDocument(url);
              document = JDOMXIncluder.resolve(document, url.toExternalForm());

              xmlDocumentsInRoot.put(name, document);
            } catch (JDOMException e) {
              System.out.println("Cannot load .xml document " + name);
            } catch (IOException e) {
              System.out.println("Cannot load .xml document " + name);
            }
          }
        }
      }
    }

    ClassPool libraryPoolsUnion = ContainerClassPool.getUnion(pluginFile.toString(), libraryPools);
    return new IdeaPlugin(pluginFile, pluginClassPool, libraryPoolsUnion, pluginXml, xmlDocumentsInRoot);
  }

  @NotNull
  private static Plugin createFromDirectory(@NotNull File directoryPath) throws BrokenPluginException, IOException {
    return createFromJars(directoryPath, getPluginJars(directoryPath));
  }

  @NotNull
  private static List<JarFile> getPluginJars(@NotNull File pluginDirectory) throws IOException, BrokenPluginException {
    final File lib = new File(pluginDirectory, "lib");
    if (!lib.isDirectory()) {
      throw new BrokenPluginException("Plugin lib is not found: " + lib);
    }

    final List<JarFile> jars = Util.getJars(lib, Predicates.<File>alwaysTrue());
    if (jars.size() == 0) {
      throw new BrokenPluginException("No jar files found under " + lib);
    }

    return jars;
  }

  @NotNull
  @Override
  public Plugin createPlugin(@NotNull File pluginFile) throws IOException, BrokenPluginException {
    if (!pluginFile.exists()) {
      throw new IOException("Plugin is not found: " + pluginFile);
    }

    if (pluginFile.isFile()) {
      String fileName = pluginFile.getName();
      if (fileName.endsWith(".zip")) {
        return createFromZip(pluginFile);
      }
      if (fileName.endsWith(".jar")) {
        return createFromJar(pluginFile);
      }
      throw new BrokenPluginException("Unknown input file type: " + pluginFile);
    }

    File[] pluginRootFiles = pluginFile.listFiles();
    if (pluginRootFiles == null || pluginRootFiles.length == 0) {
      throw new BrokenPluginException("Plugin root directory " + pluginFile + " is empty");
    }
    if (pluginRootFiles.length > 1) {
      throw new BrokenPluginException("Plugin root directory " + pluginFile + " contains more than one child \"lib\"");
    }
    return createFromDirectory(pluginFile);

  }
}
