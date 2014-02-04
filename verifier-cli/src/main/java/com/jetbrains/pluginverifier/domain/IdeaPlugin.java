package com.jetbrains.pluginverifier.domain;

import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.jetbrains.pluginverifier.pool.ClassPool;
import com.jetbrains.pluginverifier.pool.ContainerClassPool;
import com.jetbrains.pluginverifier.pool.InMemoryJarClassPool;
import com.jetbrains.pluginverifier.pool.JarClassPool;
import com.jetbrains.pluginverifier.resolvers.CombiningResolver;
import com.jetbrains.pluginverifier.resolvers.Resolver;
import com.jetbrains.pluginverifier.utils.StringUtil;
import com.jetbrains.pluginverifier.utils.Util;
import com.jetbrains.pluginverifier.utils.xml.JDOMUtil;
import com.jetbrains.pluginverifier.utils.xml.JDOMXIncluder;
import com.jetbrains.pluginverifier.utils.xml.XIncludeException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class IdeaPlugin {
  public static final String PLUGIN_XML_ENTRY_NAME = "META-INF/plugin.xml";

  private final Idea myIdea;
  private final ClassPool myPluginClassPool;
  private final ClassPool myLibraryClassPool;
  private final ClassPool myAllClassesPool;

  private final String myId;
  private Resolver myResolver;

  private Set<IdeaPlugin> dependenciesWithTransitive;
  private static final Set<IdeaPlugin> DEP_CALC_MARKER = new HashSet<IdeaPlugin>();

  private final List<PluginDependency> myDependencies;

  private final String myPluginName;

  private IdeaPlugin(Idea idea, String pluginDirectory, ClassPool pluginClassPool, ClassPool libraryClassPool,  @Nullable Document pluginXml) throws BrokenPluginException {
    myIdea = idea;
    myPluginClassPool = pluginClassPool;
    myLibraryClassPool = libraryClassPool;

    if (pluginXml == null) {
      throw new BrokenPluginException("No plugin.xml found for plugin " + pluginDirectory);
    }

    myId = getPluginId(pluginXml);
    if (myId == null) {
      throw new BrokenPluginException("No id or name in plugin.xml (" + pluginDirectory + ')');
    }

    String name = pluginXml.getRootElement().getChildTextTrim("name");
    if (Strings.isNullOrEmpty(name)) {
      name = myId;
    }
    myPluginName = name;

    myDependencies = getPluginDependencies(pluginXml);

    myAllClassesPool = ContainerClassPool.union(pluginDirectory, Arrays.asList(pluginClassPool, libraryClassPool));
  }

  public static IdeaPlugin createFromDirectory(Idea idea, File pluginDirectory) throws IOException, BrokenPluginException {
    List<JarFile> jarFiles = getPluginJars(pluginDirectory);

    Document pluginXml = null;

    ClassPool pluginClassPool = null;
    List<ClassPool> libraryPools = new ArrayList<ClassPool>();

    for (JarFile jar : jarFiles) {
      ZipEntry pluginXmlEntry = jar.getEntry(PLUGIN_XML_ENTRY_NAME);

      if (pluginXmlEntry != null) {
        if (pluginClassPool != null) {
          throw new BrokenPluginException("Plugin has more then one jar with plugin.xml : " + pluginDirectory);
        }

        pluginClassPool = new JarClassPool(jar);

        URL jarURL = new URL(
          "jar:" + StringUtil.replace(new File(jar.getName()).toURI().toASCIIString(), "!", "%21") + "!/META-INF/plugin.xml"
        );

        try {
          pluginXml = JDOMUtil.loadDocument(jarURL);
          pluginXml = JDOMXIncluder.resolve(pluginXml, jarURL.toExternalForm());
        }
        catch (JDOMException e) {
          throw new BrokenPluginException("Invalid plugin.xml", e);
        }
        catch (XIncludeException e) {
          throw new BrokenPluginException("Failed to read plugin.xml", e);
        }
      }
      else {
        libraryPools.add(new JarClassPool(jar));
      }
    }

    return new IdeaPlugin(idea, pluginDirectory.getPath(), pluginClassPool, ContainerClassPool.union(pluginDirectory.getPath(),
                                                                                                     libraryPools), pluginXml);
  }


  public static IdeaPlugin createFromZip(Idea idea, File zipFile) throws IOException, BrokenPluginException {
    byte[] pluginXmlBytes = null;
    ClassPool pluginClassPool = null;

    InMemoryJarClassPool zipRootPool = new InMemoryJarClassPool("PLUGIN ROOT");

    List<ClassPool> libraryPool = new ArrayList<ClassPool>();

    URL pluginXmlUrl = null;

    final ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));

    try {
      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        if (entry.isDirectory()) continue;

        String entryName = entry.getName();

        if (entryName.endsWith(".class")) {
          ClassNode node = new ClassNode();
          new ClassReader(zipInputStream).accept(node, 0);

          zipRootPool.addClass(node);
        }
        else if (isPluginXmlInRoot(entryName)) {
          byte[] data = ByteStreams.toByteArray(zipInputStream);

          if (pluginXmlBytes != null && !Arrays.equals(data, pluginXmlBytes)) {
            throw new BrokenPluginException("Plugin has more then one jars with plugin.xml");
          }

          pluginXmlBytes = data;
          pluginClassPool = zipRootPool;
          pluginXmlUrl = new URL(
            "jar:" + StringUtil.replace(zipFile.toURI().toASCIIString(), "!", "%21") + "!/META-INF/plugin.xml"
          );
        }
        else if (entryName.matches("([^/]+/)?lib/[^/]+\\.jar")) {
          ZipInputStream innerJar = new ZipInputStream(zipInputStream);

          InMemoryJarClassPool pool = new InMemoryJarClassPool(entryName);

          ZipEntry innerEntry;
          while ((innerEntry = innerJar.getNextEntry()) != null) {
            String name = innerEntry.getName();
            if (name.equals(PLUGIN_XML_ENTRY_NAME)) {
              byte[] data = ByteStreams.toByteArray(innerJar);

              if (pluginXmlBytes != null && !Arrays.equals(data, pluginXmlBytes)) {
                throw new BrokenPluginException("Plugin has more then one jars with plugin.xml");
              }

              pluginXmlBytes = data;
              pluginClassPool = pool;
              pluginXmlUrl = new URL("jar:" + StringUtil.replace(zipFile.toURI().toASCIIString(), "!", "%21") + "!/" + entryName + "!/META-INF/plugin.xml");
            }
            else if (name.endsWith(".class")) {
              pool.addClass(name.substring(0, name.length() - ".class".length()), ByteStreams.toByteArray(innerJar));
            }
          }

          if (pool != pluginClassPool) {
            libraryPool.add(pool);
          }
        }
      }
    }
    finally {
      zipInputStream.close();
    }

    if (pluginXmlBytes == null) {
      throw new BrokenPluginException("No plugin.xml found for plugin " + zipFile.getPath());
    }

    Document pluginXml;

    try {
      pluginXml = JDOMUtil.loadDocument(new ByteArrayInputStream(pluginXmlBytes));
      pluginXml = JDOMXIncluder.resolve(pluginXml, pluginXmlUrl.toExternalForm());
    }
    catch (JDOMException e) {
      throw new BrokenPluginException("Invalid plugin.xml", e);
    }
    catch (XIncludeException e) {
      throw new BrokenPluginException("Failed to read plugin.xml", e);
    }

    if (!zipRootPool.isEmpty()) {
      if (pluginClassPool != zipRootPool) {
        throw new BrokenPluginException("Plugin contains .class files in the root, but has no META-INF/plugin.xml");
      }
    }

    return new IdeaPlugin(idea, zipFile.getPath(), pluginClassPool, ContainerClassPool.union(zipFile.getPath(), libraryPool), pluginXml);
  }

  private static boolean isPluginXmlInRoot(String entryName) {
    if (entryName.equals(PLUGIN_XML_ENTRY_NAME)) return true;

    return entryName.endsWith(PLUGIN_XML_ENTRY_NAME) && entryName.indexOf('/') == entryName.length() - PLUGIN_XML_ENTRY_NAME.length() - 1;
  }

  @Nullable
  private static String getPluginId(@NotNull Document pluginXml) {
    final String id = pluginXml.getRootElement().getChildText("id");

    if (id == null || id.length() == 0) {
      final String name = pluginXml.getRootElement().getChildText("name");

      if (name == null || name.length() == 0) {
        return null;
      }

      return name;
    }

    return id;
  }

  private static List<PluginDependency> getPluginDependencies(Document pluginXml) {
    final List dependsElements = pluginXml.getRootElement().getChildren("depends");

    List<PluginDependency> dependencies = new ArrayList<PluginDependency>();
    for (Object dependsObj : dependsElements) {
      Element dependsElement = (Element) dependsObj;

      final boolean optional = Boolean.parseBoolean(dependsElement.getAttributeValue("optional", "false"));
      final String pluginId = dependsElement.getTextTrim();

      dependencies.add(new PluginDependency(pluginId, optional));
    }

    return dependencies;
  }

  private static List<JarFile> getPluginJars(File pluginDirectory) throws IOException {
    final File lib = new File(pluginDirectory, "lib");
    if (!lib.isDirectory())
      throw new RuntimeException("Plugin lib is not found: " + lib);

    final List<JarFile> jars = Util.getJars(lib, Predicates.<File>alwaysTrue());
    if (jars.size() == 0)
      throw new RuntimeException("No jar files found under " + lib);

    return jars;
  }

  public String getId() {
    return myId;
  }

  public List<PluginDependency> getDependencies() {
    return myDependencies;
  }

  public ClassPool getClassPool() {
    return myAllClassesPool;
  }

  public ClassPool getPluginClassPool() {
    return myPluginClassPool;
  }

  public ClassPool getLibraryClassPool() {
    return myLibraryClassPool;
  }

  public Set<IdeaPlugin> getDependenciesWithTransitive() {
    Set<IdeaPlugin> res = dependenciesWithTransitive;
    if (res == DEP_CALC_MARKER) throw new RuntimeException("Cyclic plugin dependencies");

    if (dependenciesWithTransitive == null) {
      dependenciesWithTransitive = DEP_CALC_MARKER;

      try {
        res = new HashSet<IdeaPlugin>();

        for (PluginDependency pluginDependency : getDependencies()) {
          final IdeaPlugin plugin = myIdea.getBundledPlugin(pluginDependency.getId());
          if (plugin != null) {
            if (res.add(plugin)) {
              res.addAll(plugin.getDependenciesWithTransitive());
            }
          }
        }

        dependenciesWithTransitive = res;
      }
      finally {
        if (dependenciesWithTransitive == DEP_CALC_MARKER) {
          dependenciesWithTransitive = null;
        }
      }
    }

    return res;
  }

  public Resolver getResolver() {
    if (myResolver == null) {
      List<Resolver> resolvers = new ArrayList<Resolver>();

      resolvers.add(getClassPool());
      resolvers.add(myIdea.getResolver());

      for (IdeaPlugin dep : getDependenciesWithTransitive()) {
        ClassPool pluginClassPool = dep.getPluginClassPool();
        if (!pluginClassPool.isEmpty()) {
          resolvers.add(pluginClassPool);
        }

        ClassPool libraryClassPool = dep.getLibraryClassPool();
        if (!libraryClassPool.isEmpty()) {
          resolvers.add(libraryClassPool);
        }
      }

      myResolver = CombiningResolver.union(resolvers);
    }

    return myResolver;
  }

  public Resolver getResolverOfDependencies() {
    List<Resolver> resolvers = new ArrayList<Resolver>();

    resolvers.add(myIdea.getResolver());

    for (IdeaPlugin dep : getDependenciesWithTransitive()) {
      resolvers.add(dep.getClassPool());
    }

    return CombiningResolver.union(resolvers);
  }

  public Idea getIdea() {
    return myIdea;
  }

  public String getPluginName() {
    return myPluginName;
  }
}

