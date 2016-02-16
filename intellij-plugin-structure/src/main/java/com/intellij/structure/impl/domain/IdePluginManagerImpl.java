package com.intellij.structure.impl.domain;

import com.google.common.base.Predicates;
import com.google.common.io.ByteStreams;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginManager;
import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.impl.resolvers.InMemoryJarResolver;
import com.intellij.structure.impl.utils.JarsUtils;
import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.impl.utils.xml.JDOMUtil;
import com.intellij.structure.impl.utils.xml.JDOMXIncluder;
import com.intellij.structure.impl.utils.xml.XIncludeException;
import com.intellij.structure.resolvers.Resolver;
import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.net.MalformedURLException;
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
public class IdePluginManagerImpl extends PluginManager {

  private static final String META_INF_ENTRY = "META-INF/";
  private static final String PLUGIN_XML_ENTRY_NAME = META_INF_ENTRY + "plugin.xml";
  private static final String CLASS_SUFFIX = ".class";
  private static final Pattern XML_IN_ROOT_PATTERN = Pattern.compile("([^/]*/)?META-INF/.+\\.xml");

  @NotNull
  private static IdePluginImpl createFromZip(@NotNull File zipFile) throws IOException {
    byte[] pluginXmlBytes = null;
    Resolver pluginResolver = null;
    List<Resolver> libraryPool = new ArrayList<Resolver>();
    URL pluginXmlUrl = null;
    URL mainJarUrl = null;
    Map<String, Document> allXmlInRoot = new HashMap<String, Document>();

    InMemoryJarResolver zipRootPool = new InMemoryJarResolver("Plugin root file: " + zipFile.getAbsolutePath());

    final ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));

    try {
      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        if (entry.isDirectory()) continue;

        String entryName = entry.getName();

        if (entryName.endsWith(CLASS_SUFFIX)) {
          //simply .class-file
          ClassNode node = new ClassNode();
          new ClassReader(zipInputStream).accept(node, 0);

          zipRootPool.addClass(node);

        } else if (isXmlInRoot(entryName)) {
          //some *.xml file (maybe META-INF/plugin.xml)

          final byte[] data = ByteStreams.toByteArray(zipInputStream);

          final String jarPath = "jar:" + StringUtil.replace(zipFile.toURI().toASCIIString(), "!", "%21") + "!/";

          final URL xmlUrl = new URL(
              jarPath + entryName
          );

          boolean isPluginXml = entryName.endsWith(PLUGIN_XML_ENTRY_NAME);

          if (isPluginXml) {
            //this is the main plugin.xml i.e. META-INF/plugin.xml
            try {
              /*This is for those plugins which are structured as follows (e.g. Go plugin):
              .IntelliJIDEAx0
                  plugins
                      Sample
                          lib
                              libfoo.jar
                              libbar.jar
                          classes
                              com/foo/.....
                          ...
                          META-INF
                              plugin.xml

                'entryName' would be Sample/META-INF/plugin.xml instead of META-INF/plugin.xml for correct plugins
                 so let's consider a 'Sample' directory as a "root" of the plugin
              */
              String subDirectory = StringUtil.trimEnd(entryName, PLUGIN_XML_ENTRY_NAME);
              subDirectory = StringUtil.trimEnd(subDirectory, "/");
              mainJarUrl = new URL(jarPath + (subDirectory.isEmpty() ? "" : subDirectory + "/"));
            } catch (MalformedURLException e) {
              throw new IncorrectPluginException("Plugin main .jar (containing a META-INF/plugin.xml) is broken", e);
            }

            if (pluginXmlBytes != null && !Arrays.equals(data, pluginXmlBytes)) {
              throw new IncorrectPluginException("Plugin has more than one jars with plugin.xml");
            }
            pluginXmlBytes = data;
            pluginResolver = zipRootPool;
            pluginXmlUrl = xmlUrl;
          }

          //if it is the first .xml-file
          tryAddXmlInRoot(allXmlInRoot, entryName, data, xmlUrl, isPluginXml);

        } else if (entryName.matches("([^/]+/)?lib/[^/]+\\.jar")) {
          ZipInputStream innerJar = new ZipInputStream(zipInputStream);

          final InMemoryJarResolver innerPool = new InMemoryJarResolver(entryName);
          final Map<String, Document> innerDocuments = new HashMap<String, Document>();

          ZipEntry innerEntry;
          while ((innerEntry = innerJar.getNextEntry()) != null) {
            String name = innerEntry.getName();

            if (name.startsWith(META_INF_ENTRY) && name.endsWith(".xml")) {
              //some .xml (maybe META-INF/plugin.xml

              final byte[] data = ByteStreams.toByteArray(innerJar);

              final String mainJarPath = "jar:" + StringUtil.replace(zipFile.toURI().toASCIIString(), "!", "%21") + "!/" + entryName + "!/";

              final URL xmlUrl = new URL(
                  mainJarPath + name
              );

              boolean isPluginXml = name.endsWith(PLUGIN_XML_ENTRY_NAME);
              if (isPluginXml) {
                if (pluginXmlBytes != null && !Arrays.equals(data, pluginXmlBytes)) {
                  throw new IncorrectPluginException("Plugin has more than one jars with plugin.xml");
                }

                try {
                  mainJarUrl = new URL(mainJarPath);
                } catch (MalformedURLException e) {
                  throw new IncorrectPluginException("Plugin main .jar (containing a META-INF/plugin.xml) is broken", e);
                }

                pluginXmlBytes = data; //the main plugin.xml
                pluginResolver = innerPool; //pluginPool is in this .jar exactly
                allXmlInRoot = innerDocuments; //and documents are based on this META-INF/* directory

                pluginXmlUrl = xmlUrl;
              }

              //parse a document and add to list of all xml-s
              tryAddXmlInRoot(innerDocuments, name, data, xmlUrl, isPluginXml);

            } else if (name.endsWith(CLASS_SUFFIX)) {
              innerPool.addClass(name.substring(0, name.length() - CLASS_SUFFIX.length()), ByteStreams.toByteArray(innerJar));
            }
          }

          if (innerPool != pluginResolver) {
            libraryPool.add(innerPool);
          }
        }
      }
    } finally {
      IOUtils.closeQuietly(zipInputStream);
    }

    if (pluginXmlBytes == null) {
      throw new IncorrectPluginException("No META-INF/plugin.xml found for plugin " + zipFile.getPath());
    }
    //pluginXmlUrl is also not null

    Document pluginXml;

    try {
      pluginXml = JDOMUtil.loadDocument(new ByteArrayInputStream(pluginXmlBytes));
      pluginXml = JDOMXIncluder.resolve(pluginXml, pluginXmlUrl.toExternalForm());
    } catch (JDOMException e) {
      throw new IncorrectPluginException("Invalid META-INF/plugin.xml", e);
    } catch (XIncludeException e) {
      throw new IncorrectPluginException("Failed to read META-INF/plugin.xml", e);
    }

    if (!zipRootPool.isEmpty()) {
      if (pluginResolver != zipRootPool) {
        throw new IncorrectPluginException("Plugin contains .class files in the root, but has no META-INF/plugin.xml");
      }
    }

    return new IdePluginImpl(mainJarUrl, pluginResolver, Resolver.getUnion(zipFile.getPath(), libraryPool), pluginXml, allXmlInRoot);
  }

  private static void tryAddXmlInRoot(@NotNull Map<String, Document> container,
                                      @NotNull String entryName,
                                      @NotNull byte[] data,
                                      @NotNull URL xmlUrl,
                                      boolean isPluginXml) throws IncorrectPluginException {
    try {
      Document document = JDOMUtil.loadDocument(new ByteArrayInputStream(data));
      document = JDOMXIncluder.resolve(document, xmlUrl.toExternalForm());
      container.put(entryName, document);
    } catch (Exception e) {
      if (isPluginXml) {
        //plugin.xml should be correct!
        throw new IncorrectPluginException("Failed to read and parse META-INF/plugin.xml", e);
      }
      //for non-main .xml it's not critical
//      System.err.println("Couldn't parse " + entryName);
    }
  }

  private static boolean isXmlInRoot(@NotNull String entryName) {
    return XML_IN_ROOT_PATTERN.matcher(entryName).matches();
  }

  @NotNull
  private static Plugin createFromJar(@NotNull File jarFile) throws IOException {
    return createFromJars(jarFile, Collections.singletonList(new JarFile(jarFile)));
  }

  private static Plugin createFromJars(@NotNull File pluginFile,
                                       @NotNull List<JarFile> jarFiles) throws IOException, IncorrectPluginException {
    Document pluginXml = null;
    Resolver pluginResolver = null;
    List<Resolver> libraryPools = new ArrayList<Resolver>();
    URL mainJarUrl = null;

    for (JarFile jar : jarFiles) {
      ZipEntry pluginXmlEntry = jar.getEntry(PLUGIN_XML_ENTRY_NAME);
      if (pluginXmlEntry != null) {
        if (pluginResolver != null) {
          throw new IncorrectPluginException("Plugin has more than one .jar with plugin.xml " + pluginFile);
        }

        pluginResolver = Resolver.createJarClassPool(jar);

        final String jarPath = "jar:" + StringUtil.replace(new File(jar.getName()).toURI().toASCIIString(), "!", "%21") + "!/";

        try {
          mainJarUrl = new URL(jarPath);
        } catch (MalformedURLException e) {
          throw new IncorrectPluginException("Plugin main .jar (containing a META-INF/plugin.xml) is broken", e);
        }

        URL pluginXmlUrl = new URL(
            jarPath + "META-INF/plugin.xml"
        );

        try {
          pluginXml = JDOMUtil.loadDocument(pluginXmlUrl);
          pluginXml = JDOMXIncluder.resolve(pluginXml, pluginXmlUrl.toExternalForm());
        } catch (JDOMException e) {
          throw new IncorrectPluginException("Invalid plugin.xml", e);
        } catch (XIncludeException e) {
          throw new IncorrectPluginException("Failed to read plugin.xml", e);
        }
      } else {
        libraryPools.add(Resolver.createJarClassPool(jar));
      }
    }

    if (pluginXml == null) {
      throw new IncorrectPluginException("No META-INF/plugin.xml found for plugin " + pluginFile);
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
              //this is not so important for minor XML-s
//              System.err.println("Cannot load .xml document " + name);
            } catch (IOException e) {
              //this is not so important for minor XML-s
//              System.err.println("Cannot load .xml document " + name);
            }
          }
        }
      }
    }

    Resolver libraryPoolsUnion = Resolver.getUnion(pluginFile.toString(), libraryPools);
    return new IdePluginImpl(mainJarUrl, pluginResolver, libraryPoolsUnion, pluginXml, xmlDocumentsInRoot);
  }

  @NotNull
  private static Plugin createFromDirectory(@NotNull File directoryPath) throws IOException, IncorrectPluginException {
    return createFromJars(directoryPath, getPluginJars(directoryPath));
  }

  @NotNull
  private static List<JarFile> getPluginJars(@NotNull File pluginDirectory) throws IOException, IncorrectPluginException {
    final File lib = new File(pluginDirectory, "lib");
    if (!lib.isDirectory()) {
      throw new IncorrectPluginException("Plugin \"lib\" directory is not found (should be " + lib + ")");
    }

    final List<JarFile> jars = JarsUtils.getJars(lib, Predicates.<File>alwaysTrue());
    if (jars.size() == 0) {
      throw new IncorrectPluginException("No jar files found under \"lib\" directory" + "(should be under " + lib + ")");
    }

    return jars;
  }

  @NotNull
  @Override
  public Plugin createPlugin(@NotNull File pluginFile) throws IOException, IncorrectPluginException {
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
      throw new IncorrectPluginException("Incorrect plugin file type " + pluginFile + ". Should be one of .zip or .jar archives");
    }

    File[] pluginRootFiles = pluginFile.listFiles();
    if (pluginRootFiles == null || pluginRootFiles.length == 0) {
      throw new IncorrectPluginException("Plugin root directory " + pluginFile + " is empty");
    }
/*
    TODO: should be proceed?
    if (pluginRootFiles.length > 1) {
      throw new IncorrectStructureException("Plugin root directory " + pluginFile + " contains more than one child \"lib\"");
    }
*/
    return createFromDirectory(pluginFile);
  }
}
